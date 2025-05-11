# main.py
from datetime import datetime, timedelta, timezone
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from jose import JWTError, jwt
from passlib.context import CryptContext
from pydantic import BaseModel, EmailStr
import smtplib
import json
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from cryptography.fernet import Fernet
from typing import Optional
from pydantic import field_validator
import imaplib
import email
import bcrypt
from email.header import decode_header
import re
from email.utils import parsedate_to_datetime
from fastapi import Query



# 🔐 Configuració de seguretat
SECRET_KEY = "j85jctq0ba8w7kkfvb3i0i43yhkcv421"  # Genera amb: openssl rand -hex 32
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

# Llegeix la clau des del fitxer (enlloc de generar-la)
with open("fernet.key", "rb") as key_file:
    KEY = key_file.read()

cipher_suite = Fernet(KEY)
# Configuració SMTP per defecte
DEFAULT_SMTP_PORT = 587

# 🔧 Inicialització
app = FastAPI()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")


# 📦 Models
class UserBase(BaseModel):
    username: str
    email: EmailStr
    smtp_server: str
    smtp_port: int = DEFAULT_SMTP_PORT
    smtp_username: str
    imap_server: str  
    imap_port: int = 993  
    imap_username: str 


    @field_validator('smtp_port')
    def validate_port(cls, v):
        if not (1 <= v <= 65535):
            raise ValueError("Port SMTP invàlid")
        return v

    @field_validator('smtp_server')
    def validate_server(cls, v):
        if '.' not in v:
            raise ValueError("Servidor SMTP invàlid")
        return v
    
    @field_validator('imap_server')
    def validate_imap_server(cls, v):
        if '.' not in v:
            raise ValueError("Servidor IMAP invàlid")
        return v

    @field_validator('imap_port')
    def validate_imap_port(cls, v):
        if not (1 <= v <= 65535):
            raise ValueError("Port IMAP invàlid")
        return v

class UserCreate(UserBase):
    password: str
    smtp_password: str
    imap_password: str

class UserInDB(UserBase):
    hashed_password: str
    encrypted_smtp_password: str
    encrypted_imap_password: str

class EmailRequest(BaseModel):
    to: EmailStr
    subject: str
    body: str

class RefreshTokenRequest(BaseModel):
    refresh_token: str

# 📁 Utilitats
def load_users():
    try:
        with open("users.json", "r") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return {}

def save_users(users):
    with open("users.json", "w") as f:
        json.dump(users, f, indent=2)

def encrypt_password(password: str) -> str:
    return cipher_suite.encrypt(password.encode()).decode()

def decrypt_password(encrypted_password: str) -> str:
    return cipher_suite.decrypt(encrypted_password.encode()).decode()

# 🔐 Autenticació
def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

async def get_current_user(token: str = Depends(oauth2_scheme)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="No s'han pogut validar les credencials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    
    users = load_users()
    if username not in users:
        raise credentials_exception
    
    return UserInDB(**users[username])

def create_refresh_token(data: dict):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(days=7)  # 7 dies de validesa
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)



def decode_subject(subject):
    decoded_parts = []
    for part, encoding in decode_header(subject):
        if isinstance(part, bytes):
            decoded_parts.append(part.decode(encoding or "utf-8"))
        else:
            decoded_parts.append(part)
    return "".join(decoded_parts)

def clean_preview(text):
    text = re.sub(r'\s+', ' ', text)  # Elimina espais múltiples
    text = re.sub(r'<[^>]+>', '', text)  # Elimina HTML
    return text.strip()

def parse_email_date(date_str):
    return parsedate_to_datetime(date_str).isoformat()

def extract_preview(msg):
    # Si el missatge és multipart, busca la part text/plain
    if msg.is_multipart():
        for part in msg.walk():
            content_type = part.get_content_type()
            content_disposition = str(part.get("Content-Disposition"))
            if content_type == "text/plain" and "attachment" not in content_disposition:
                try:
                    return part.get_payload(decode=True).decode(errors="ignore")[:200]
                except Exception:
                    continue
        # Si no troba text/plain, agafa la primera part que pugui
        for part in msg.walk():
            try:
                return part.get_payload(decode=True).decode(errors="ignore")[:200]
            except Exception:
                continue
        return ""
    else:
        # Missatge no multipart
        try:
            return msg.get_payload(decode=True).decode(errors="ignore")[:200]
        except Exception:
            return ""

# 🔐 Endpoints
@app.post("/register", status_code=201)
def register(user: UserCreate):
    users = load_users()
    
    if user.username in users:
        raise HTTPException(status_code=400, detail="El nom d'usuari ja existeix")
    
    if any(u["email"] == user.email for u in users.values()):
        raise HTTPException(status_code=400, detail="L'email ja està registrat")
    
    hashed_password = bcrypt.hashpw(user.password.encode(), bcrypt.gensalt()).decode()
    encrypted_smtp_password = encrypt_password(user.smtp_password)
    encrypted_imap_password = encrypt_password(user.imap_password)
    
    users[user.username] = {
        "username": user.username,  
        "hashed_password": hashed_password,
        "email": user.email,
        "smtp_server": user.smtp_server,
        "smtp_port": user.smtp_port,
        "smtp_username": user.smtp_username,
        "encrypted_smtp_password": encrypted_smtp_password,
        "imap_server": user.imap_server,
        "imap_port": user.imap_port,
        "imap_username": user.imap_username,
        "encrypted_imap_password": encrypted_imap_password,
    }
    
    
    save_users(users)
    return {"message": "Usuari registrat correctament"}

@app.post("/login")
def login(form_data: OAuth2PasswordRequestForm = Depends()):
    users = load_users()
    user_data = users.get(form_data.username)
    
    if not bcrypt.checkpw(form_data.password.encode(), user_data["hashed_password"].encode()):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credencials incorrectes",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = create_access_token(data={"sub": form_data.username})
    refresh_token = create_refresh_token(data={"sub": form_data.username})
    
    return {
        "access_token": access_token,
        "refresh_token": refresh_token,  
        "token_type": "bearer"
    }

@app.post("/send-email")
def send_email(request: EmailRequest, current_user: UserInDB = Depends(get_current_user)):
    try:
        smtp_password = decrypt_password(current_user.encrypted_smtp_password)
        
        msg = MIMEMultipart()
        msg["From"] = current_user.email
        msg["To"] = request.to
        msg["Subject"] = request.subject
        msg.attach(MIMEText(request.body, "html"))
        
        with smtplib.SMTP(current_user.smtp_server, current_user.smtp_port) as server:
            server.starttls()
            server.login(current_user.smtp_username, smtp_password)
            server.send_message(msg)
            
        return {"message": "Correu enviat correctament"}
    
    except smtplib.SMTPAuthenticationError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Error d'autenticació SMTP. Verifica les credencials SMTP."
        )
    except smtplib.SMTPException as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Error del servidor SMTP: {str(e)}"
        )
    except Exception as e:
        
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error intern del servidor: {str(e)}"
        )

# Informació de l'usuari
@app.get("/me")
def get_user_info(current_user: UserInDB = Depends(get_current_user)):
    return {
        "username": current_user.username,
        "email": current_user.email,
        "smtp_server": current_user.smtp_server,
        "smtp_port": current_user.smtp_port
    }

@app.post("/refresh-token")
def refresh_token(request: RefreshTokenRequest):
    try:
        payload = jwt.decode(
            request.refresh_token,
            SECRET_KEY,
            algorithms=[ALGORITHM]
        )
        username: str = payload.get("sub")
        if not username:
            raise HTTPException(status_code=401, detail="Token invàlid")
        users = load_users()
        if username not in users:
            raise HTTPException(status_code=404, detail="Usuari no trobat")
        # Genera nou access token
        new_access_token = create_access_token(data={"sub": username})
        # Genera nou refresh token (opcional, però recomanat per seguretat)
        new_refresh_token = create_refresh_token(data={"sub": username})
        return {
            "access_token": new_access_token,
            "refresh_token": new_refresh_token,  
            "token_type": "bearer"
        }
    except JWTError:
        raise HTTPException(status_code=401, detail="Token de refresc invàlid")


        
@app.get("/emails")
def get_emails(
    current_user: UserInDB = Depends(get_current_user),
    folder: str = "INBOX",
    limit: int = 10
):
    try:
        imap_password = decrypt_password(current_user.encrypted_imap_password)
        
        # Connexió IMAP
        mail = imaplib.IMAP4_SSL(current_user.imap_server, current_user.imap_port)
        mail.login(current_user.imap_username, imap_password)
        mail.select(folder)

        # Obtenir últims correus
        status, data = mail.uid('search', None, "ALL") 
        mail_ids = data[0].split()
        latest_ids = mail_ids[-limit:]
        
        emails = []
        for uid in mail_ids[-10:]:
            status, msg_data = mail.uid('fetch', uid, '(RFC822)')
            msg = email.message_from_bytes(msg_data[0][1])
            
            emails.append({
                "uid": uid.decode(),
                "from": msg["From"],
                "subject": decode_subject(msg["Subject"]),
                "date": parse_email_date(msg["Date"]),
                "preview": clean_preview(extract_preview(msg))
            })
        
        mail.logout()
        return {"emails": emails}

    except imaplib.IMAP4.error as e:
        raise HTTPException(status_code=401, detail=f"Error IMAP: {str(e)}")
    
@app.delete("/emails/delete")
def delete_email(
    uid: str = Query(..., description="UID del correu a eliminar"),
    current_user: UserInDB = Depends(get_current_user)
):
    try:
        imap_password = decrypt_password(current_user.encrypted_imap_password)
        mail = imaplib.IMAP4_SSL(current_user.imap_server, current_user.imap_port)
        mail.login(current_user.imap_username, imap_password)
        
        mail.select("INBOX", readonly=False)
        
        result, _ = mail.uid('STORE', uid, '+FLAGS', '\\Deleted')
        if result != 'OK':
            raise HTTPException(status_code=400, detail="Error en marcar com eliminat")
        
        result, _ = mail.expunge()
        if result != 'OK':
            raise HTTPException(status_code=500, detail="Error en expunge")
        
        return {"message": "Correu eliminat correctament"}
    except imaplib.IMAP4.error as e:
        raise HTTPException(status_code=401, detail=f"Error IMAP: {str(e)}")