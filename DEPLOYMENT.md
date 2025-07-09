# GCP Deployment Guide

This guide will help you deploy the Ohana Backend API to Google Cloud Platform.

## Prerequisites

1. **Google Cloud SDK** installed and configured
2. **Docker** installed
3. **GCP Project** with billing enabled
4. **Cloud SQL** instance (MySQL) set up
5. **Service Account** with appropriate permissions

## Setup Steps

### 1. Enable Required APIs

```bash
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com
gcloud services enable sqladmin.googleapis.com
```

### 2. Set Up Cloud SQL

Create a MySQL instance in Cloud SQL:

```bash
gcloud sql instances create ohana-db \
    --database-version=MYSQL_8_0 \
    --tier=db-f1-micro \
    --region=us-central1 \
    --root-password=your-root-password
```

Create a database:

```bash
gcloud sql databases create ohana --instance=ohana-db
```

Create a user:

```bash
gcloud sql users create ohana_user \
    --instance=ohana-db \
    --password=your-user-password
```

### 3. Configure Environment Variables

Set the following environment variables before deployment:

```bash
export DB_HOST="your-cloud-sql-ip"
export DB_NAME="ohana"
export DB_USER="ohana_user"
export DB_PASSWORD="your-user-password"
export JWT_SECRET="your-secure-jwt-secret"
```

### 4. Deploy Using the Script

```bash
./deploy.sh
```

### 5. Manual Deployment (Alternative)

If you prefer manual deployment:

```bash
# Build the application
./gradlew build

# Build and push Docker image
docker build -t gcr.io/ohana-464419/ohana-backend .
docker push gcr.io/ohana-464419/ohana-backend

# Deploy to Cloud Run
gcloud run deploy ohana-backend \
    --image gcr.io/ohana-464419/ohana-backend \
    --region us-central1 \
    --platform managed \
    --allow-unauthenticated \
    --port 4242 \
    --memory 512Mi \
    --cpu 1 \
    --max-instances 10 \
    --set-env-vars "DB_HOST=$DB_HOST,DB_NAME=$DB_NAME,DB_USER=$DB_USER,JWT_SECRET=$JWT_SECRET" \
    --set-env-vars "DB_PASSWORD=$DB_PASSWORD"
```

## Database Migration

Before deploying, ensure your database schema is up to date. You can run Flyway migrations:

```bash
# Set up Flyway with your Cloud SQL connection
flyway -url="jdbc:mysql://your-cloud-sql-ip:3306/ohana" \
       -user="ohana_user" \
       -password="your-user-password" \
       migrate
```

## Security Considerations

1. **Service Account Key**: The `key.json` file should be stored securely and not committed to version control
2. **Database Password**: Use strong passwords and consider using Secret Manager
3. **JWT Secret**: Use a cryptographically secure random string
4. **Network Security**: Configure Cloud SQL to only accept connections from your Cloud Run service

## Monitoring and Logging

- **Cloud Run Logs**: View logs in the GCP Console or using `gcloud logs read`
- **Cloud SQL Monitoring**: Monitor database performance in the Cloud SQL console
- **Application Metrics**: Consider adding Prometheus/Grafana for custom metrics

## Troubleshooting

### Common Issues

1. **Database Connection**: Ensure Cloud SQL instance is running and accessible
2. **Port Configuration**: Verify the application listens on port 4242
3. **Environment Variables**: Check that all required environment variables are set
4. **Memory Issues**: Increase memory allocation if the application crashes

### Useful Commands

```bash
# View service logs
gcloud logs read --service=ohana-backend --limit=50

# Check service status
gcloud run services describe ohana-backend --region=us-central1

# Update environment variables
gcloud run services update ohana-backend \
    --region=us-central1 \
    --set-env-vars "NEW_VAR=value"
```

## Cost Optimization

- Use `db-f1-micro` for development/testing
- Set appropriate `--max-instances` to control scaling
- Monitor usage in the GCP Console
- Consider using Cloud Run's free tier (2 million requests/month)
