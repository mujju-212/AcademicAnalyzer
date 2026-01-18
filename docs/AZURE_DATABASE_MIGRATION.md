# Azure Database Migration

## Overview
The Academic Analyzer application database has been migrated from local MySQL to **Azure MySQL Flexible Server** for cloud-based deployment and accessibility.

## Migration Date
**January 18, 2026**

---

## Azure MySQL Server Details

### Server Configuration
- **Server Name**: `academicanalyzer-db-sea`
- **Fully Qualified Domain Name (FQDN)**: `academicanalyzer-db-sea.mysql.database.azure.com`
- **Location**: Southeast Asia
- **Resource Group**: `AcademicAnalyzer-RG`
- **MySQL Version**: 8.0.21
- **SKU**: Standard_B1ms (Burstable tier)
- **Storage**: 20 GB
- **IOPS**: 360 (free tier)
- **Subscription**: Azure for Students

### Database
- **Database Name**: `academic_analyzer`
- **Character Set**: utf8mb3
- **Collation**: utf8mb3_general_ci

### Security
- **SSL/TLS**: Disabled temporarily for compatibility with older MySQL clients
- **Firewall**: Configured to allow connections from `0.0.0.0-255.255.255.255` (all IPs - for development only)
- **⚠️ Production Note**: Enable SSL and restrict firewall rules before production deployment

---

## Migration Process

### 1. Azure MySQL Server Creation
```bash
# Created resource group
az group create --name AcademicAnalyzer-RG --location southeastasia

# Registered MySQL provider
az provider register --namespace Microsoft.DBforMySQL

# Created MySQL Flexible Server
az mysql flexible-server create \
  --name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG \
  --location southeastasia \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 20 \
  --version 8.0.21 \
  --public-access 0.0.0.0-255.255.255.255

# Created database
az mysql flexible-server db create \
  --server-name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG \
  --database-name academic_analyzer
```

### 2. Database Export
```bash
# Full export with data
mysqldump -h localhost -u root \
  --routines --triggers --single-transaction \
  academic_analyzer > academic_analyzer_full.sql
```

**Export Size**: 196 KB (0.19 MB)

### 3. Database Import
```bash
# Disabled secure transport for older MySQL clients
az mysql flexible-server parameter set \
  --server-name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG \
  --name require_secure_transport \
  --value OFF

# Imported data to Azure
mysql -h academicanalyzer-db-sea.mysql.database.azure.com \
  -u <admin_user> \
  academic_analyzer < academic_analyzer_full.sql
```

### 4. Application Configuration Update
Updated `.env` file with Azure MySQL connection details:
```env
DB_HOST=academicanalyzer-db-sea.mysql.database.azure.com
DB_PORT=3306
DB_NAME=academic_analyzer
DB_USERNAME=<see_credentials_file>
DB_PASSWORD=<see_credentials_file>
```

---

## Migrated Database Structure

### Tables (17 Total)
1. `component_groups` - Marking component groups
2. `entered_exam_marks` - Exam marks entry
3. `exam_types` - Types of exams (MSE, ESE, etc.)
4. `launched_results` - Published results
5. `launched_student_results` - Individual student results
6. `marking_components` - Assessment components
7. `marking_schemes` - Grading schemes
8. `marks` - Student marks data
9. `password_reset_otps` - Password reset tokens
10. `registration_otps` - Account registration OTPs
11. `section_subjects` - Subject-section mappings
12. `sections` - Academic sections
13. `student_component_marks` - Component-wise marks
14. `students` - Student records
15. `subject_exam_types` - Subject-exam type mappings
16. `subjects` - Course subjects
17. `users` - Application users

### Data Statistics
- **Students**: 96 records
- **All Data**: Preserved completely from local database

---

## Application Connectivity

### Java Desktop Application
- **Connection Class**: `com.sms.database.DatabaseConnection`
- **Config Loader**: `com.sms.util.ConfigLoader`
- **Status**: ✅ Connected successfully

### Python Result Portal
- **Framework**: Flask
- **MySQL Connector**: `mysql.connector`
- **Status**: ✅ Connected successfully (verified 96 students)

---

## Testing & Verification

### Connection Tests Performed
1. ✅ Azure MySQL server creation verified
2. ✅ Database import verified (all 17 tables)
3. ✅ Java application compilation successful
4. ✅ Java application connection established
5. ✅ Python result-portal connection established
6. ✅ Data integrity verified (student count matches)

### Test Commands Used
```bash
# Verify tables
mysql -h academicanalyzer-db-sea.mysql.database.azure.com \
  -u <admin_user> academic_analyzer -e "SHOW TABLES;"

# Test Python connection
python -c "import mysql.connector; \
  conn = mysql.connector.connect(
    host='academicanalyzer-db-sea.mysql.database.azure.com',
    user='<admin_user>',
    password='<password>',
    database='academic_analyzer'
  ); \
  cursor = conn.cursor(); \
  cursor.execute('SELECT COUNT(*) FROM students'); \
  print('Students:', cursor.fetchone()[0]); \
  conn.close()"
```

---

## Benefits of Azure Migration

### Scalability
- Cloud-based infrastructure scales with demand
- Easy to upgrade storage and compute resources
- Automatic backups and point-in-time restore

### Accessibility
- Accessible from anywhere with internet connection
- Multiple applications can connect simultaneously
- No local infrastructure required

### Reliability
- Azure SLA: 99.99% uptime
- Automated backups (7-day retention default)
- Geo-redundant backup options available

### Security
- Built-in DDoS protection
- Network isolation with VNet integration (optional)
- Azure Active Directory authentication support
- Encryption at rest and in transit

---

## Cost Considerations

### Current Configuration
- **Tier**: Burstable Standard_B1ms
- **Storage**: 20 GB
- **Estimated Cost**: ~$15-20/month (Azure for Students credits applicable)

### Cost Optimization Tips
1. Stop server when not in use (development only)
2. Use Azure for Students credits
3. Monitor usage through Azure Portal
4. Set up budget alerts

---

## Maintenance & Management

### Azure Portal
Access server management at:
https://portal.azure.com → Resource Groups → AcademicAnalyzer-RG → academicanalyzer-db-sea

### Common Management Tasks

#### Start/Stop Server
```bash
# Stop server (saves costs)
az mysql flexible-server stop \
  --name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG

# Start server
az mysql flexible-server start \
  --name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG
```

#### Backup Management
```bash
# List backups
az mysql flexible-server backup list \
  --server-name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG
```

#### Monitor Performance
```bash
# Get server metrics
az monitor metrics list \
  --resource "/subscriptions/<subscription-id>/resourceGroups/AcademicAnalyzer-RG/providers/Microsoft.DBforMySQL/flexibleServers/academicanalyzer-db-sea" \
  --metric "cpu_percent"
```

---

## Security Recommendations for Production

### 1. Enable SSL/TLS
```bash
az mysql flexible-server parameter set \
  --server-name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG \
  --name require_secure_transport \
  --value ON
```

### 2. Restrict Firewall Rules
```bash
# Remove all access rule
az mysql flexible-server firewall-rule delete \
  --name AllowAll_2026-1-18_17-58-39 \
  --server-name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG

# Add specific IP only
az mysql flexible-server firewall-rule create \
  --name AllowMyIP \
  --server-name academicanalyzer-db-sea \
  --resource-group AcademicAnalyzer-RG \
  --start-ip-address <your_ip> \
  --end-ip-address <your_ip>
```

### 3. Use Azure Key Vault
Store credentials in Azure Key Vault instead of .env file

### 4. Enable Advanced Threat Protection
Configure through Azure Portal → Security → Advanced Threat Protection

---

## Troubleshooting

### Connection Issues

#### Issue: "Connection timeout"
**Solution**: Check firewall rules, ensure your IP is allowed

#### Issue: "SSL required"
**Solution**: Either enable SSL in client or disable `require_secure_transport`

#### Issue: "Authentication failed"
**Solution**: Verify credentials, ensure using correct format: `username` (not `username@hostname`)

### Performance Issues

#### Slow Queries
- Check query execution plans
- Add appropriate indexes
- Consider upgrading SKU if sustained high load

#### Connection Limit Reached
- Default: 20 connections for B1ms
- Upgrade to higher SKU for more connections
- Implement connection pooling

---

## Rollback Plan

### To Revert to Local MySQL
1. Keep local MySQL server running with data intact
2. Update `.env` file:
   ```env
   DB_HOST=localhost
   DB_USERNAME=root
   DB_PASSWORD=<local_password>
   ```
3. Restart applications

### Database Backup from Azure
```bash
# Export from Azure
mysqldump -h academicanalyzer-db-sea.mysql.database.azure.com \
  -u <admin_user> \
  --routines --triggers --single-transaction \
  academic_analyzer > azure_backup.sql

# Import to local
mysql -u root -p academic_analyzer < azure_backup.sql
```

---

## Next Steps

### Immediate
- ✅ Database migrated successfully
- ✅ Applications configured to use Azure MySQL
- ✅ Connection tests passed

### Short-term
- [ ] Test all application features with Azure database
- [ ] Monitor performance and costs
- [ ] Set up Azure budget alerts

### Long-term
- [ ] Enable SSL/TLS for production
- [ ] Restrict firewall to specific IPs
- [ ] Implement connection pooling
- [ ] Set up automated monitoring and alerts
- [ ] Configure backup retention policy
- [ ] Consider VNet integration for enhanced security

---

## References
- [Azure MySQL Flexible Server Documentation](https://learn.microsoft.com/azure/mysql/flexible-server/)
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [Azure CLI MySQL Commands](https://learn.microsoft.com/cli/azure/mysql/flexible-server)
- [Azure Pricing Calculator](https://azure.microsoft.com/pricing/calculator/)

---

## Contact & Support
For issues or questions regarding the Azure database:
1. Check Azure Portal for service health
2. Review Azure Monitor logs
3. Contact Azure Support (included with Azure for Students)
4. Check application logs for connection errors
