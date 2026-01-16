# Student Result Portal

Web portal for students to view their academic results with OTP authentication.

## Features

- 🔐 **OTP Authentication** - Secure login using email OTP
- 📊 **Result Viewing** - Complete academic results with metrics
- 📈 **Performance Graphs** - Visual analytics using Chart.js
- 📥 **PDF Download** - Download results as formatted PDF
- 📱 **Responsive Design** - Works on mobile and desktop

## Setup Instructions

### Prerequisites

- Python 3.8 or higher
- MySQL database (already configured in main project)
- Internet connection for Chart.js CDN

### Installation

1. **Navigate to the portal directory**:
   ```bash
   cd result-portal
   ```

2. **Create a virtual environment** (recommended):
   ```bash
   python -m venv venv
   ```

3. **Activate the virtual environment**:
   - Windows:
     ```bash
     venv\Scripts\activate
     ```
   - Linux/Mac:
     ```bash
     source venv/bin/activate
     ```

4. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

5. **Configure environment variables**:
   - The portal uses the same `.env` file from the parent project
   - Ensure the following variables are set in `../.env`:
     ```
     DB_HOST=localhost
     DB_PORT=3306
     DB_NAME=academic_analyzer
     DB_USER=your_db_user
     DB_PASSWORD=your_db_password
     
     MAILERSEND_API_KEY=your_api_key
     MAILERSEND_FROM_EMAIL=noreply@your-domain.com
     MAILERSEND_FROM_NAME=Academic Analyzer
     ```

### Running the Portal

1. **Start the Flask server**:
   ```bash
   python app.py
   ```

2. **Access the portal**:
   - Open your browser and go to: `http://localhost:5000`

3. **Default port**: 5000
   - To change the port, edit `app.py` and modify:
     ```python
     app.run(debug=True, host='0.0.0.0', port=YOUR_PORT)
     ```

## Usage Guide

### For Students

1. **Select Section**:
   - Open the portal
   - Select your section from the dropdown
   - Enter your roll number

2. **Request OTP**:
   - Click "Request OTP"
   - Check your registered email for the 6-digit code
   - OTP expires in 10 minutes

3. **Verify OTP**:
   - Enter the 6-digit OTP
   - Click "Verify & View Results"

4. **View Results**:
   - See your complete academic results
   - View performance metrics (Total, %, CGPA, Grade, Status)
   - Explore subject-wise breakdown
   - View performance graphs
   - Download PDF of your results

### For Administrators

- Results must be launched from the main Academic Analyzer application
- Once launched, results are stored in `student_web_results` table
- Students can view results for all launches in their section

## Architecture

```
result-portal/
├── app.py                 # Flask backend with API endpoints
├── requirements.txt       # Python dependencies
├── README.md             # This file
├── static/
│   └── style.css         # Responsive CSS styling
└── templates/
    ├── index.html        # Section selection & OTP verification
    └── results.html      # Result display with charts
```

## API Endpoints

### `/api/sections` (GET)
- Returns list of sections with active launched results

### `/api/request-otp` (POST)
- Generates and sends OTP to student's email
- Payload: `{section_id, roll_number}`

### `/api/verify-otp` (POST)
- Verifies OTP and returns student ID
- Payload: `{section_id, roll_number, otp}`

### `/api/launched-results/<student_id>` (GET)
- Returns all launched results for a student

### `/results/<student_id>` (GET)
- Renders results page with charts

### `/api/download-pdf/<launch_id>/<student_id>` (GET)
- Generates and downloads PDF of specific result

## Database Tables Used

- `sections` - Section information
- `students` - Student details (name, roll number, email)
- `launched_results` - Launch metadata
- `student_web_results` - Individual student results (JSON format)

## Security Notes

- OTP stored in-memory (use Redis for production)
- OTP expires after 10 minutes
- No session management (stateless design)
- Email validation required for OTP delivery

## Troubleshooting

### "Failed to connect to server"
- Ensure Flask app is running
- Check if port 5000 is available
- Verify firewall settings

### "Failed to send OTP"
- Check MailerSend API configuration in `.env`
- Verify student email exists in database
- Check MailerSend domain verification

### "No results found"
- Ensure results have been launched from main app
- Check `student_web_results` table has data
- Verify student belongs to selected section

### Charts not displaying
- Check internet connection (Chart.js uses CDN)
- Verify JavaScript is enabled in browser
- Check browser console for errors

## Technology Stack

- **Backend**: Flask (Python)
- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **Charts**: Chart.js 4.4.0
- **PDF**: ReportLab
- **Database**: MySQL 8.x
- **Email**: MailerSend API

## Future Enhancements

- [ ] Add Redis for OTP storage
- [ ] Implement session management
- [ ] Add result comparison across semesters
- [ ] Email notifications when results are launched
- [ ] Mobile app integration
- [ ] Multi-language support
- [ ] Dark mode theme

## Support

For issues or questions, please contact the Academic Analyzer development team.

---

**Note**: This portal is designed to work with the Academic Analyzer Java application. Ensure the main application is properly configured and results are launched before students can view them.
