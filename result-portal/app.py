"""
Student Result Portal - Flask Backend
Allows students to view their launched results
"""

from flask import Flask, render_template, request, jsonify, send_file
import mysql.connector
import json
from datetime import datetime
import io
from reportlab.lib.pagesizes import letter, A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer, PageBreak
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv('../.env')

app = Flask(__name__)
app.secret_key = 'your-secret-key-here-change-in-production'

# Database configuration from .env
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'database': os.getenv('DB_NAME', 'academic_analyzer'),
    'user': os.getenv('DB_USERNAME', 'root'),
    'password': os.getenv('DB_PASSWORD', '')
}

def get_db_connection():
    """Get database connection"""
    return mysql.connector.connect(**DB_CONFIG)

def get_grade(percentage):
    """Calculate grade from percentage"""
    if percentage >= 85:
        return 'A+'
    elif percentage >= 75:
        return 'A'
    elif percentage >= 65:
        return 'B+'
    elif percentage >= 55:
        return 'B'
    elif percentage >= 45:
        return 'C'
    elif percentage >= 40:
        return 'D'
    else:
        return 'F'

@app.route('/')
def index():
    """Landing page - Section selection"""
    return render_template('index.html')

@app.route('/api/sections', methods=['GET'])
def get_sections():
    """Get all sections that have launched results"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        query = """
            SELECT DISTINCT s.id, s.section_name, s.academic_year, s.semester,
                   COUNT(DISTINCT lr.id) as launch_count
            FROM sections s
            JOIN launched_results lr ON s.id = lr.section_id
            WHERE lr.status = 'active'
            GROUP BY s.id, s.section_name, s.academic_year, s.semester
            ORDER BY s.section_name
        """
        
        cursor.execute(query)
        sections = cursor.fetchall()
        
        cursor.close()
        conn.close()
        
        return jsonify({'success': True, 'sections': sections})
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/verify-student', methods=['POST'])
def verify_student():
    """Verify student credentials and redirect to results"""
    try:
        data = request.json
        roll_number = data.get('roll_number')
        section_id = data.get('section_id')
        
        if not roll_number or not section_id:
            return jsonify({'success': False, 'error': 'Roll number and section required'}), 400
        
        # Get student details
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        query = """
            SELECT id, student_name, email, roll_number
            FROM students
            WHERE roll_number = %s AND section_id = %s
        """
        
        cursor.execute(query, (roll_number, section_id))
        student = cursor.fetchone()
        
        cursor.close()
        conn.close()
        
        if not student:
            return jsonify({'success': False, 'error': 'Student not found in selected section'}), 404
        
        return jsonify({'success': True, 'student_id': student['id'], 'student_name': student['student_name']})
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
        
        query = """
            SELECT id, student_name, email, roll_number
            FROM students
            WHERE roll_number = %s AND section_id = %s
        """
        cursor.execute(query, (roll_number, section_id))
        student = cursor.fetchone()
        
        cursor.close()
        conn.close()
        
        if not student:
            return jsonify({'success': False, 'error': 'Student not found'}), 404
        
        if not student['email']:
            return jsonify({'success': False, 'error': 'No email registered for this student'}), 400
        
        # Generate OTP
        otp = generate_otp()
        
        # Store OTP with expiry (10 minutes)
        otp_key = f"{roll_number}_{section_id}"
        otp_storage[otp_key] = {
            'otp': otp,
            'student_id': student['id'],
            'expires': datetime.now() + timedelta(minutes=10)
        }
        
        # TODO: Send OTP via email (integrate with MailerSend)
        # For now, just return OTP in response (REMOVE IN PRODUCTION)
        print(f"OTP for {student['student_name']}: {otp}")
        
        return jsonify({
            'success': True,
            'message': 'OTP sent to your registered email',
            'email_hint': student['email'][:3] + '***@' + student['email'].split('@')[1],
            'debug_otp': otp  # REMOVE IN PRODUCTION
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/verify-otp', methods=['POST'])
def verify_otp():
    """Verify OTP and return session token"""
    try:
        data = request.json
        roll_number = data.get('roll_number')
        section_id = data.get('section_id')
        otp = data.get('otp')
        
        otp_key = f"{roll_number}_{section_id}"
        
        if otp_key not in otp_storage:
            return jsonify({'success': False, 'error': 'OTP not found or expired'}), 400
        
        stored_data = otp_storage[otp_key]
        
        # Check expiry
        if datetime.now() > stored_data['expires']:
            del otp_storage[otp_key]
            return jsonify({'success': False, 'error': 'OTP expired'}), 400
        
        # Verify OTP
        if stored_data['otp'] != otp:
            return jsonify({'success': False, 'error': 'Invalid OTP'}), 400
        
        # OTP verified - remove from storage
        student_id = stored_data['student_id']
        del otp_storage[otp_key]
        
        return jsonify({
            'success': True,
            'student_id': student_id,
            'message': 'OTP verified successfully'
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/launched-results/<int:student_id>', methods=['GET'])
def get_launched_results(student_id):
    """Get all active launched results for a student"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        query = """
            SELECT lr.id, lr.launch_name, lr.launch_date, lr.status,
                   s.section_name, lr.show_rank, lr.show_class_stats,
                   lr.show_component_marks, lr.show_subject_details,
                   swr.result_data
            FROM launched_results lr
            JOIN student_web_results swr ON lr.id = swr.launch_id
            JOIN sections s ON lr.section_id = s.id
            WHERE swr.student_id = %s AND lr.status = 'active'
            ORDER BY lr.launch_date DESC
        """
        
        cursor.execute(query, (student_id,))
        results = cursor.fetchall()
        
        # Parse JSON result_data for each result
        for result in results:
            if result['result_data']:
                result['result_data'] = json.loads(result['result_data'])
        
        cursor.close()
        conn.close()
        
        return jsonify({'success': True, 'results': results})
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/debug/<int:student_id>')
def debug_results(student_id):
    """Debug endpoint to view raw JSON data"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        # Get all launched results for this student
        query = """
            SELECT lr.id as launch_id, lr.launch_name, lr.launch_date, lr.status,
                   s.section_name, s.academic_year, s.semester,
                   swr.result_data
            FROM launched_results lr
            JOIN launched_student_results swr ON lr.id = swr.launch_id
            JOIN sections s ON lr.section_id = s.id
            WHERE swr.student_id = %s AND lr.status = 'active'
            ORDER BY lr.launch_date DESC
            LIMIT 1
        """
        
        cursor.execute(query, (student_id,))
        result = cursor.fetchone()
        
        cursor.close()
        conn.close()
        
        if result and result['result_data']:
            result_data = json.loads(result['result_data'])
            return f"<pre>{json.dumps(result_data, indent=2)}</pre>"
        else:
            return "No result data found"
            
    except Exception as e:
        return f"Error: {str(e)}"

@app.route('/results/<int:student_id>')
def view_results(student_id):
    """Results page"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        # Get student information
        cursor.execute("""
            SELECT id, student_name, roll_number, email
            FROM students
            WHERE id = %s
        """, (student_id,))
        student = cursor.fetchone()
        
        if not student:
            return "Student not found", 404
        
        # Get all launched results for this student
        query = """
            SELECT lr.id as launch_id, lr.launch_name, lr.launch_date, lr.status,
                   s.section_name, s.academic_year, s.semester,
                   swr.result_data
            FROM launched_results lr
            JOIN launched_student_results swr ON lr.id = swr.launch_id
            JOIN sections s ON lr.section_id = s.id
            WHERE swr.student_id = %s AND lr.status = 'active'
            ORDER BY lr.launch_date DESC
        """
        
        cursor.execute(query, (student_id,))
        results = cursor.fetchall()
        
        # Parse JSON result_data and format for display
        launched_results = []
        for result in results:
            if result['result_data']:
                result_data = json.loads(result['result_data'])
                
                # Extract data from new JSON structure
                overall = result_data.get('overall', {})
                subjects = result_data.get('subjects', [])
                
                # Format the result for template
                formatted_result = {
                    'launch_id': result['launch_id'],
                    'launch_name': result['launch_name'],
                    'launch_date': result['launch_date'].strftime('%d/%m/%Y %H:%M') if result['launch_date'] else 'N/A',
                    'section_name': result['section_name'],
                    'academic_year': result['academic_year'],
                    'semester': result['semester'],
                    'status': 'Passed' if overall.get('is_passing', False) else 'Failed',
                    'total_marks': overall.get('total_obtained', 0),
                    'total_max_marks': overall.get('total_max', 0),
                    'percentage': overall.get('percentage', 0),
                    'cgpa': overall.get('cgpa', 0),
                    'grade': overall.get('grade', 'N/A'),
                    'subjects': subjects  # Pass subjects directly from new JSON structure
                }
                
                launched_results.append(formatted_result)
        
        cursor.close()
        conn.close()
        
        return render_template('results.html', 
                             student={'student_id': student['id'], 
                                    'name': student['student_name'], 
                                    'roll_number': student['roll_number']},
                             launched_results=launched_results)
        
    except Exception as e:
        return f"Error loading results: {str(e)}", 500

@app.route('/api/download-pdf/<int:launch_id>/<int:student_id>', methods=['GET'])
def download_pdf(launch_id, student_id):
    """Generate and download PDF of results"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        # Get result data
        query = """
            SELECT lr.launch_name, lr.launch_date, lr.allow_pdf_download,
                   s.section_name, s.academic_year, s.semester,
                   st.student_name, st.roll_number,
                   swr.result_data
            FROM launched_results lr
            JOIN launched_student_results swr ON lr.id = swr.launch_id
            JOIN students st ON swr.student_id = st.id
            JOIN sections s ON lr.section_id = s.id
            WHERE lr.id = %s AND swr.student_id = %s
        """
        
        cursor.execute(query, (launch_id, student_id))
        result = cursor.fetchone()
        
        cursor.close()
        conn.close()
        
        if not result:
            return jsonify({'success': False, 'error': 'Result not found'}), 404
        
        if not result['allow_pdf_download']:
            return jsonify({'success': False, 'error': 'PDF download not allowed'}), 403
        
        # Parse result data
        result_data = json.loads(result['result_data'])
        
        # Generate PDF
        buffer = io.BytesIO()
        pdf = generate_result_pdf(buffer, result, result_data)
        buffer.seek(0)
        
        filename = f"Result_{result['student_name'].replace(' ', '_')}_{result['launch_name'].replace(' ', '_')}.pdf"
        
        return send_file(
            buffer,
            mimetype='application/pdf',
            as_attachment=True,
            download_name=filename
        )
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

def generate_result_pdf(buffer, result, result_data):
    """Generate PDF report with charts"""
    doc = SimpleDocTemplate(buffer, pagesize=A4,
                           rightMargin=50, leftMargin=50,
                           topMargin=50, bottomMargin=30)
    
    elements = []
    styles = getSampleStyleSheet()
    
    # Title
    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Heading1'],
        fontSize=22,
        textColor=colors.HexColor('#667eea'),
        spaceAfter=20,
        alignment=TA_CENTER,
        fontName='Helvetica-Bold'
    )
    
    elements.append(Paragraph("Academic Result Report", title_style))
    elements.append(Spacer(1, 12))
    
    # Student Info - Get academic year and semester from result (from sections table)
    student_info = result_data.get('student_info', {})
    
    info_data = [
        ['Student Name:', student_info.get('name', result['student_name'])],
        ['Roll Number:', result['roll_number']],
        ['Section:', result['section_name']],
        ['Academic Year:', str(result.get('academic_year', 'N/A'))],
        ['Semester:', str(result.get('semester', 'N/A'))],
        ['Launch Date:', result['launch_date'].strftime('%d %B %Y')]
    ]
    
    info_table = Table(info_data, colWidths=[2*inch, 4*inch])
    info_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#f0f0f0')),
        ('TEXTCOLOR', (0, 0), (-1, -1), colors.black),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('FONTNAME', (0, 0), (0, -1), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 10),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.grey)
    ]))
    
    elements.append(info_table)
    elements.append(Spacer(1, 20))
    
    # Overall Results
    overall = result_data.get('overall', {})
    status_text = 'PASSED' if overall.get('is_passing', False) else 'FAILED'
    status_color = colors.green if overall.get('is_passing', False) else colors.red
    
    overall_data = [
        ['Total Marks', 'Max Marks', 'Percentage', 'CGPA', 'Grade', 'Status'],
        [
            str(int(overall.get('total_obtained', 0))),
            str(int(overall.get('total_max', 0))),
            f"{overall.get('percentage', 0):.2f}%",
            f"{overall.get('cgpa', 0):.2f}",
            overall.get('grade', 'N/A'),
            status_text
        ]
    ]
    
    overall_table = Table(overall_data, colWidths=[1.25*inch]*6)
    overall_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#667eea')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, 0), 10),
        ('FONTSIZE', (0, 1), (-1, -1), 10),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
        ('BACKGROUND', (0, 1), (-1, -1), colors.beige),
        ('TEXTCOLOR', (5, 1), (5, 1), status_color),
        ('FONTNAME', (5, 1), (5, 1), 'Helvetica-Bold'),
        ('GRID', (0, 0), (-1, -1), 1, colors.black)
    ]))
    
    elements.append(Paragraph("Overall Performance", styles['Heading2']))
    elements.append(Spacer(1, 12))
    elements.append(overall_table)
    elements.append(Spacer(1, 20))
    
    # Subject-wise Results
    subjects = result_data.get('subjects', [])
    if subjects:
        elements.append(Paragraph("Subject-wise Performance", styles['Heading2']))
        elements.append(Spacer(1, 12))
        
        subject_data = [['Subject', 'Weighted Total', 'Max Marks', 'Percentage', 'Grade', 'Status']]
        
        for subject in subjects:
            weighted_total = int(subject.get('weighted_total', 0))
            max_marks = subject.get('max_marks', 100)
            percentage = (weighted_total / max_marks * 100) if max_marks > 0 else 0
            status_text = '✓ Pass' if subject.get('passed', False) else '✗ Fail'
            
            subject_data.append([
                subject.get('subject_name', 'N/A'),
                str(weighted_total),
                str(max_marks),
                f"{percentage:.2f}%",
                subject.get('grade', 'N/A'),
                status_text
            ])
        
        subject_table = Table(subject_data, colWidths=[2.2*inch, 1.2*inch, 1*inch, 1.1*inch, 0.7*inch, 0.8*inch])
        subject_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#667eea')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 9),
            ('FONTSIZE', (0, 1), (-1, -1), 8),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
            ('TOPPADDING', (0, 0), (-1, -1), 6),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.lightgrey]),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey)
        ]))
        
        elements.append(subject_table)
        elements.append(Spacer(1, 15))
        
        # Add detailed exam types for each subject
        elements.append(Paragraph("Detailed Exam-wise Breakdown", styles['Heading2']))
        elements.append(Spacer(1, 12))
        
        for subject in subjects:
            # Subject name
            subject_style = ParagraphStyle(
                'SubjectStyle',
                parent=styles['Heading3'],
                fontSize=12,
                textColor=colors.HexColor('#667eea'),
                spaceAfter=8,
                spaceBefore=10
            )
            elements.append(Paragraph(subject.get('subject_name', 'N/A'), subject_style))
            
            # Exam types table
            exam_types = subject.get('exam_types', [])
            if exam_types:
                exam_data = [['Exam Component', 'Obtained', 'Max', 'Weightage', 'Weighted Marks']]
                
                for exam in exam_types:
                    obtained = exam.get('obtained', 0)
                    max_marks = exam.get('max', 0)
                    weightage = exam.get('weightage', 0)
                    weighted = (obtained / max_marks * weightage) if max_marks > 0 else 0
                    
                    exam_data.append([
                        exam.get('exam_name', 'N/A'),
                        str(obtained),
                        str(max_marks),
                        f"{weightage}%",
                        f"{weighted:.2f}"
                    ])
                
                # Add total row
                weighted_total = int(subject.get('weighted_total', 0))
                max_marks_total = subject.get('max_marks', 100)
                exam_data.append([
                    'TOTAL',
                    '',
                    '',
                    '',
                    f"{weighted_total}/{max_marks_total}"
                ])
                
                exam_table = Table(exam_data, colWidths=[2.5*inch, 1*inch, 0.9*inch, 1*inch, 1.3*inch])
                exam_table.setStyle(TableStyle([
                    ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#667eea')),
                    ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                    ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                    ('FONTSIZE', (0, 0), (-1, 0), 9),
                    ('FONTSIZE', (0, 1), (-1, -1), 8),
                    ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
                    ('TOPPADDING', (0, 0), (-1, -1), 6),
                    ('ROWBACKGROUNDS', (0, 1), (-1, -2), [colors.white, colors.lightgrey]),
                    ('BACKGROUND', (0, -1), (-1, -1), colors.HexColor('#f0f0f0')),
                    ('FONTNAME', (0, -1), (-1, -1), 'Helvetica-Bold'),
                    ('LINEABOVE', (0, -1), (-1, -1), 1, colors.black),
                    ('GRID', (0, 0), (-1, -1), 0.5, colors.grey)
                ]))
                
                elements.append(exam_table)
                elements.append(Spacer(1, 10))
        
        # Add performance charts on new page
        elements.append(PageBreak())
        elements.append(Paragraph("Performance Analysis", styles['Heading2']))
        elements.append(Spacer(1, 15))
        
        try:
            from reportlab.graphics.shapes import Drawing, String
            from reportlab.graphics.charts.barcharts import VerticalBarChart
            from reportlab.graphics.charts.piecharts import Pie
            
            # Chart 1: Bar Chart - Subject Performance
            drawing1 = Drawing(500, 200)
            chart1 = VerticalBarChart()
            chart1.x = 30
            chart1.y = 40
            chart1.width = 440
            chart1.height = 140
            
            chart1.data = [[int(s.get('weighted_total', 0)) for s in subjects]]
            chart1.categoryAxis.categoryNames = [s.get('subject_name', 'N/A')[:20] for s in subjects]
            chart1.categoryAxis.labels.angle = 20
            chart1.categoryAxis.labels.fontSize = 8
            chart1.categoryAxis.labels.dy = -8
            chart1.valueAxis.valueMin = 0
            chart1.valueAxis.valueMax = 100
            chart1.valueAxis.valueStep = 20
            
            # Gradient colors for bars
            bar_colors = [colors.HexColor('#667eea'), colors.HexColor('#764ba2'), 
                         colors.HexColor('#f093fb'), colors.HexColor('#4facfe'),
                         colors.HexColor('#00f2fe'), colors.HexColor('#43e97b')]
            for i in range(len(subjects)):
                chart1.bars[(i, 0)].fillColor = bar_colors[i % len(bar_colors)]
            
            title1 = String(250, 180, 'Subject-wise Performance', textAnchor='middle', 
                          fontSize=12, fillColor=colors.HexColor('#333333'), fontName='Helvetica-Bold')
            drawing1.add(title1)
            drawing1.add(chart1)
            elements.append(drawing1)
            elements.append(Spacer(1, 25))
            
            # Chart 2: Pie Chart - Grade Distribution  
            drawing2 = Drawing(250, 220)
            pie2 = Pie()
            pie2.x = 55
            pie2.y = 50
            pie2.width = 140
            pie2.height = 140
            
            # Count grades
            grade_counts = {}
            for subject in subjects:
                grade = subject.get('grade', 'N/A')
                grade_counts[grade] = grade_counts.get(grade, 0) + 1
            
            pie2.data = list(grade_counts.values())
            pie2.labels = [f"{grade}" for grade in grade_counts.keys()]
            pie2.slices.strokeWidth = 1
            pie2.slices.strokeColor = colors.white
            
            # Colorful gradient scheme
            pie_colors = [colors.HexColor('#667eea'), colors.HexColor('#f093fb'), 
                         colors.HexColor('#4facfe'), colors.HexColor('#43e97b'),
                         colors.HexColor('#fa709a'), colors.HexColor('#fee140')]
            for i in range(len(pie2.data)):
                pie2.slices[i].fillColor = pie_colors[i % len(pie_colors)]
                pie2.slices[i].popout = 5 if i == 0 else 0
            
            title2 = String(125, 200, 'Grade Distribution', textAnchor='middle', 
                          fontSize=12, fillColor=colors.HexColor('#333333'), fontName='Helvetica-Bold')
            drawing2.add(title2)
            drawing2.add(pie2)
            
            # Chart 3: Doughnut Chart - Pass/Fail Ratio (side by side with pie)
            drawing3 = Drawing(250, 220)
            pie3 = Pie()
            pie3.x = 55
            pie3.y = 50
            pie3.width = 140
            pie3.height = 140
            
            # Count pass/fail
            passed_count = sum(1 for s in subjects if s.get('passed', False))
            failed_count = len(subjects) - passed_count
            
            pie3.data = [passed_count, failed_count] if failed_count > 0 else [passed_count]
            pie3.labels = ['Pass', 'Fail'] if failed_count > 0 else ['Pass']
            pie3.slices.strokeWidth = 1
            pie3.slices.strokeColor = colors.white
            
            # Make it doughnut style with inner radius
            pie3.innerRadiusPercent = 50
            
            # Green for pass, red for fail
            pie3.slices[0].fillColor = colors.HexColor('#43e97b')
            if failed_count > 0:
                pie3.slices[1].fillColor = colors.HexColor('#fa709a')
            
            title3 = String(125, 200, 'Pass/Fail Status', textAnchor='middle', 
                          fontSize=12, fillColor=colors.HexColor('#333333'), fontName='Helvetica-Bold')
            drawing3.add(title3)
            drawing3.add(pie3)
            
            # Place pie charts side by side in a table
            charts_table = Table([[drawing2, drawing3]], colWidths=[250, 250])
            elements.append(charts_table)
            
        except Exception as e:
            print(f"Error generating charts: {e}")
            import traceback
            traceback.print_exc()
    
    # Build PDF
    doc.build(elements)
    return doc

if __name__ == '__main__':
    print("=" * 50)
    print("Student Result Portal Starting...")
    print("=" * 50)
    print(f"Database: {DB_CONFIG['database']}")
    print(f"Host: {DB_CONFIG['host']}:{DB_CONFIG['port']}")
    print("=" * 50)
    print("\nServer running at: http://localhost:5000")
    print("Press Ctrl+C to stop")
    print("=" * 50)
    
    app.run(debug=True, host='0.0.0.0', port=5000)
