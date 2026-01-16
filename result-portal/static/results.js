// Result Portal JavaScript - Handles charts and PDF download
// Updated to work with detailed subject-wise exam breakdown

/**
 * Initialize charts for a specific result card
 * @param {number} index - Result card index for unique chart IDs
 * @param {object} resultData - Result data from backend
 */
function initializeCharts(index, resultData) {
    initializeSubjectChart(index, resultData);
    initializeExamTypeChart(index, resultData);
    initializePassFailChart(index, resultData);
}

/**
 * Initialize Subject Performance Bar Chart
 */
function initializeSubjectChart(index, resultData) {
    const ctx = document.getElementById(`subjectChart${index}`);
    if (!ctx) return;
    
    const subjects = resultData.subjects || [];
    
    if (subjects.length === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No subject data available</p>';
        return;
    }
    
    // Extract subject names and weighted totals
    const labels = subjects.map(s => s.subject_name || 'Unknown');
    const dataPoints = subjects.map(s => parseFloat(s.weighted_total) || 0);
    const maxPoints = subjects.map(() => 100); // Each subject out of 100
    
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Obtained',
                    data: dataPoints,
                    backgroundColor: 'rgba(102, 126, 234, 0.8)',
                    borderColor: 'rgba(102, 126, 234, 1)',
                    borderWidth: 2,
                    borderRadius: 5
                },
                {
                    label: 'Max (100)',
                    data: maxPoints,
                    backgroundColor: 'rgba(200, 200, 200, 0.3)',
                    borderColor: 'rgba(200, 200, 200, 0.5)',
                    borderWidth: 1,
                    borderRadius: 5
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'Subject-wise Performance',
                    font: { size: 14, weight: 'bold' },
                    padding: { top: 10, bottom: 15 }
                },
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        boxWidth: 15,
                        padding: 10,
                        font: { size: 11 }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const subjectIndex = context.dataIndex;
                            const subject = subjects[subjectIndex];
                            if (context.datasetIndex === 0) {
                                return `Obtained: ${context.parsed.y.toFixed(0)}/100 (${subject.grade || 'N/A'})`;
                            }
                            return `Max: ${context.parsed.y}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        font: { size: 10 }
                    },
                    title: {
                        display: true,
                        text: 'Marks',
                        font: { size: 11 }
                    }
                },
                x: {
                    ticks: {
                        font: { size: 10 },
                        maxRotation: 45,
                        minRotation: 0
                    },
                    title: {
                        display: true,
                        text: 'Subjects',
                        font: { size: 11 }
                    }
                }
            }
        }
    });
}

/**
 * Initialize Exam Type Distribution Doughnut Chart
 */
function initializeExamTypeChart(index, resultData) {
    const ctx = document.getElementById(`examTypeChart${index}`);
    if (!ctx) return;
    
    const subjects = resultData.subjects || [];
    
    if (subjects.length === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No exam type data available</p>';
        return;
    }
    
    // Aggregate marks by exam type across all subjects
    const examTypeAggregation = {};
    
    subjects.forEach(subject => {
        const examTypes = subject.exam_types || [];
        examTypes.forEach(exam => {
            const examName = exam.exam_name || 'Unknown';
            const obtained = parseFloat(exam.obtained) || 0;
            
            if (!examTypeAggregation[examName]) {
                examTypeAggregation[examName] = 0;
            }
            examTypeAggregation[examName] += obtained;
        });
    });
    
    // Filter out zero values and prepare chart data
    const labels = [];
    const dataPoints = [];
    
    Object.entries(examTypeAggregation).forEach(([name, value]) => {
        if (value > 0) {
            labels.push(name);
            dataPoints.push(value);
        }
    });
    
    if (dataPoints.length === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No exam marks available</p>';
        return;
    }
    
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: dataPoints,
                backgroundColor: [
                    'rgba(102, 126, 234, 0.8)',
                    'rgba(118, 75, 162, 0.8)',
                    'rgba(237, 100, 166, 0.8)',
                    'rgba(255, 154, 158, 0.8)',
                    'rgba(250, 208, 196, 0.8)',
                    'rgba(99, 205, 218, 0.8)',
                    'rgba(205, 220, 57, 0.8)'
                ],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                title: {
                    display: true,
                    text: 'Marks Distribution by Exam Type',
                    font: { size: 16 }
                },
                legend: {
                    position: 'bottom'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            return `${label}: ${value.toFixed(2)} marks (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Initialize Pass/Fail Overview Pie Chart
 */
function initializePassFailChart(index, resultData) {
    const ctx = document.getElementById(`passFailChart${index}`);
    if (!ctx) return;
    
    const subjects = resultData.subjects || [];
    
    if (subjects.length === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No pass/fail data available</p>';
        return;
    }
    
    // Count passed and failed subjects
    let passedCount = 0;
    let failedCount = 0;
    
    subjects.forEach(subject => {
        if (subject.passed === true || subject.passed === 'true') {
            passedCount++;
        } else {
            failedCount++;
        }
    });
    
    if (passedCount === 0 && failedCount === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No pass/fail data available</p>';
        return;
    }
    
    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: ['Passed', 'Failed'],
            datasets: [{
                data: [passedCount, failedCount],
                backgroundColor: [
                    'rgba(76, 175, 80, 0.8)',
                    'rgba(244, 67, 54, 0.8)'
                ],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                title: {
                    display: true,
                    text: `Pass/Fail Overview (${passedCount}/${subjects.length} Passed)`,
                    font: { size: 16 }
                },
                legend: {
                    position: 'bottom'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = passedCount + failedCount;
                            const percentage = ((value / total) * 100).toFixed(1);
                            return `${label}: ${value} subjects (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Download PDF for a specific launch and student
 * @param {number} launchId - Launch ID
 * @param {number} studentId - Student ID
 */
function downloadPDF(launchId, studentId) {
    const btn = event.target;
    const originalText = btn.textContent;
    btn.textContent = '⏳ Generating PDF...';
    btn.disabled = true;

    fetch(`/api/download-pdf/${launchId}/${studentId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to generate PDF');
            }
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `Result_${studentId}_${launchId}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            
            btn.textContent = originalText;
            btn.disabled = false;
        })
        .catch(error => {
            alert('Failed to download PDF. Please try again.');
            btn.textContent = originalText;
            btn.disabled = false;
            console.error('Error:', error);
        });
}

// Export functions for use in HTML templates
window.initializeCharts = initializeCharts;
window.downloadPDF = downloadPDF;
