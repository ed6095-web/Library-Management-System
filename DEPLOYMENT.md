# Library Management System - Deployment Guide

## Overview
This guide explains how to deploy the Library Management System with:
- **Backend**: Java Jetty server on Railway or Render
- **Frontend**: Static site on Vercel

---

## Part 1: Deploy Backend to Railway

### Prerequisites
- GitHub account (already done ✓)
- Railway account (https://railway.app)

### Steps

1. **Create Railway Account**
   - Go to https://railway.app and sign up
   - Connect your GitHub account

2. **Create New Project**
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Select `Library-Management-System` repository

3. **Configure Environment Variables**
   - In Railway dashboard, go to Variables
   - Add these environment variables:
     ```
     DB_HOST=your_database_host
     DB_USER=your_database_user
     DB_PASSWORD=your_database_password
     DB_NAME=library_management
     PORT=8080
     ```
   - Note: You may need to set up a MySQL database separately (Railway or external)

4. **Deploy**
   - Railway will automatically detect the Procfile
   - Deployment will start automatically
   - Wait for build to complete
   - Note down your backend URL (e.g., `https://your-app-backend.railway.app`)

5. **Update Database Connection**
   - Update `backend/src/DBConnection.java` with deployed database credentials
   - Commit and push changes

---

## Part 2: Deploy Frontend to Vercel

### Prerequisites
- Vercel account (https://vercel.com)
- GitHub account (already done ✓)

### Steps

1. **Create Vercel Account**
   - Go to https://vercel.com and sign up
   - Connect your GitHub account

2. **Import Project**
   - Click "Add New"
   - Select "Project"
   - Select `Library-Management-System` repository
   - Configure:
     - **Framework**: Other (static)
     - **Root Directory**: `frontend`
     - Click "Deploy"

3. **Configure API Endpoint**
   - After deployment, go to project Settings
   - Add Environment Variable:
     ```
     NEXT_PUBLIC_API_URL=https://your-backend-railway-url/api
     ```
   - Redeploy to apply changes

4. **Alternative: Set API URL in Browser**
   - If frontend is already deployed, open browser console
   - Run: `localStorage.setItem('apiBaseUrl', 'https://your-backend-url/api')`
   - Refresh the page

5. **Get Frontend URL**
   - Your frontend URL will be displayed (e.g., `https://library-management.vercel.app`)

---

## Part 3: Configure CORS (if needed)

If you get CORS errors, update `backend/src/Main.java`:

```java
// Add CORS headers
context.addFilter(new Filter() {
    @Override
    public void init(FilterConfig config) {}
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.addHeader("Access-Control-Allow-Origin", "*");
        httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {}
}, "/*", EnumSet.of(DispatcherType.REQUEST));
```

---

## Part 4: Update Frontend Configuration

In `frontend/config.js`, update the fallback URL:

```javascript
return 'https://your-backend-railway-url/api'; // Replace with actual URL
```

---

## Quick Links

- **Railway Dashboard**: https://railway.app/dashboard
- **Vercel Dashboard**: https://vercel.com/dashboard
- **Repository**: https://github.com/ed6095-web/Library-Management-System

---

## Troubleshooting

### Backend won't start
- Check database connection variables
- Verify MySQL is running and accessible
- Check Railway logs for errors

### API calls not working
- Check browser console for CORS errors
- Verify backend URL in config.js matches deployed URL
- Check Network tab in DevTools to see actual API calls

### Frontend deployment fails
- Ensure `frontend` is set as Root Directory in Vercel
- Check that config.js is in frontend directory

---

## Next Steps

1. Push these configuration files to GitHub
2. Deploy backend to Railway
3. Deploy frontend to Vercel
4. Test the application end-to-end
