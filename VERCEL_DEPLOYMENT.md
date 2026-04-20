# Vercel Deployment Guide for Library Management System

## ⚡ Quick Deployment Steps

### Step 1: Push Your Code to GitHub

```bash
git add .
git commit -m "Prepare for Vercel deployment"
git push origin main
```

### Step 2: Deploy Frontend to Vercel

1. **Go to Vercel** (https://vercel.com)
2. **Sign up/Login** with GitHub account
3. **Click "New Project"**
4. **Select your GitHub repository** (`Library-Management-System`)
5. **Configure Project:**
   - **Framework Preset**: Other (Static)
   - **Root Directory**: `frontend` (important!)
   - **Build Command**: Leave empty (static site)
   - **Output Directory**: `/` 
   - Click **Deploy**

### Step 3: Set Environment Variables in Vercel

1. **After initial deployment, go to Project Settings**
2. **Navigate to Environment Variables**
3. **Add the following:**
   - **Key**: `NEXT_PUBLIC_API_URL`
   - **Value**: `https://your-backend-url.railway.app/api` (replace with your actual backend URL)
   - Click **Save**
4. **Redeploy** (Settings → Deployments → Redeploy)

### Step 4: Deploy Backend Separately

The backend cannot run on Vercel (Java not supported). Deploy to **Render** or **Railway**:

#### Option A: Deploy to Render (Recommended)
1. Go to https://render.com
2. Click **New +** → **Web Service**
3. Connect GitHub repository
4. **Configure:**
   - **Name**: `library-management-backend`
   - **Runtime**: Docker
   - **Build Command**: `javac -cp "lib/*" -d out backend/src/*.java`
   - **Start Command**: `java -cp "out:lib/*" backend.src.Main`
5. **Add Environment Variables:**
   ```
   PORT=8080
   DB_HOST=your_database_host
   DB_USER=your_database_user
   DB_PASSWORD=your_database_password
   DB_NAME=library_management
   ```
6. Click **Deploy**

#### Option B: Deploy to Railway
1. Go to https://railway.app
2. Click **New Project** → **Deploy from GitHub repo**
3. Select your repository
4. Add environment variables (same as above)
5. Deploy automatically deploys on commit

### Step 5: Configure Frontend to Use Backend URL

After backend is deployed:

1. **Copy your backend URL** (e.g., `https://library-management-backend.onrender.com`)
2. **In Vercel Dashboard:**
   - Go to your project
   - Settings → Environment Variables
   - Update `NEXT_PUBLIC_API_URL` with actual backend URL
   - Redeploy the project
3. **Or in Browser Console** (temporary):
   ```javascript
   localStorage.setItem('apiBaseUrl', 'https://your-backend-url.railway.app/api')
   location.reload()
   ```

### Step 6: Test Your Deployment

1. **Open your Vercel URL** (e.g., `https://your-app.vercel.app`)
2. **Open Browser Console** (F12)
3. **Check for errors**
4. **Try logging in** with test credentials
5. **Verify backend connectivity**

---

## 📋 Project Configuration Files

### vercel.json
```json
{
  "buildCommand": "echo 'Static frontend'",
  "outputDirectory": "frontend",
  "routes": [
    {
      "src": "/(.+)",
      "dest": "/index.html",
      "status": 200
    }
  ]
}
```

### .vercelignore
Excludes backend files from Vercel build:
```
backend/
lib/
*.java
Dockerfile
Procfile
```

---

## 🔧 Troubleshooting

### 1. **Frontend shows "API not configured"**
   - Check browser console
   - Go to Vercel → Settings → Environment Variables
   - Verify `NEXT_PUBLIC_API_URL` is set
   - Redeploy the project

### 2. **CORS Errors when calling backend**
   - Backend needs CORS headers configured
   - Update backend `AuthServlet.java`, `BookServlet.java`, etc.:
   ```java
   response.setHeader("Access-Control-Allow-Origin", "*");
   response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
   response.setHeader("Access-Control-Allow-Headers", "Content-Type");
   ```

### 3. **Backend not responding**
   - Verify backend is deployed to Render/Railway
   - Check backend logs for errors
   - Verify environment variables are set correctly in backend
   - Test backend directly: `curl https://your-backend-url.railway.app/api/status`

### 4. **Database connection errors**
   - Backend needs database credentials in environment variables
   - Set `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` in Render/Railway
   - Ensure database is accessible from deployment platform

---

## 📚 Database Setup

### Option 1: Use Managed Database
- **Render**: Offers PostgreSQL
- **Railway**: Offers MySQL

### Option 2: External Database
- AWS RDS
- Google Cloud SQL
- DigitalOcean Managed Database
- Your own hosted MySQL

**Update `backend/src/DBConnection.java` with credentials**

---

## 🚀 Automatic Deployments

Both Vercel and Render/Railway support **automatic deployments**:
- Push to GitHub `main` branch
- Vercel automatically rebuilds frontend
- Render/Railway automatically rebuilds backend
- No manual deployment needed!

---

## 📊 Production Checklist

- [ ] Frontend deployed to Vercel
- [ ] Backend deployed to Render/Railway  
- [ ] Environment variables configured
- [ ] Database connected and initialized
- [ ] CORS headers added to backend
- [ ] Tested login functionality
- [ ] Tested CRUD operations
- [ ] Check both console for errors
- [ ] Monitor application in production

---

## 🔗 Useful Links

- **Vercel Docs**: https://vercel.com/docs
- **Render Docs**: https://render.com/docs
- **Railway Docs**: https://docs.railway.app
- **Your Vercel Dashboard**: https://vercel.com/dashboard
- **Your Render Dashboard**: https://dashboard.render.com

---

## 💡 Tips

1. **Frontend-only testing**: Update `config.js` to use `http://localhost:8080/api` for local testing
2. **Staging environment**: Create a separate branch (e.g., `staging`) for Vercel
3. **Monitor logs**: Use Vercel and Render dashboards to monitor deployment logs
4. **Custom domain**: Configure custom domain in Vercel Settings → Domains
5. **Security**: Never commit `.env` files; use Environment Variables in dashboard

