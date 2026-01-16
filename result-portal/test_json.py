import mysql.connector
import json

conn = mysql.connector.connect(
    host='localhost',
    user='root',
    password='mk0492',
    database='academic_analyzer'
)

cursor = conn.cursor(dictionary=True)
cursor.execute('SELECT result_data FROM launched_student_results WHERE id = 13')
row = cursor.fetchone()

try:
    data = json.loads(row['result_data'])
    print('✓ JSON parsed successfully')
    print(f'Subjects count: {len(data.get("subjects", []))}')
    print(f'Student: {data.get("student_info", {}).get("name")}')
    print(f'CGPA: {data.get("overall", {}).get("cgpa")}')
except Exception as e:
    print(f'✗ JSON parsing error: {e}')
    print(f'JSON length: {len(row["result_data"])}')
    print('First 500 chars:', row['result_data'][:500])

cursor.close()
conn.close()
