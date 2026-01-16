// Result Portal JavaScript - Handles charts and PDF download
// Updated with responsive design for desktop and mobile

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
    const maxPoints = subjects.map(() => 100);
    
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
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'Marks Distribution by Exam Type',
                    font: { size: 14, weight: 'bold' },
                    padding: { top: 10, bottom: 15 }
                },
                legend: {
                    position: 'bottom',
                    labels: {
                        boxWidth: 12,
                        padding: 8,
                        font: { size: 10 }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            return `${label}: ${value.toFixed(0)} marks`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Initialize Pass/Fail Pie Chart
 */
function initializePassFailChart(index, resultData) {
    const ctx = document.getElementById(`passFailChart${index}`);
    if (!ctx) return;
    
    const subjects = resultData.subjects || [];
    
    if (subjects.length === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No subject data available</p>';
        return;
    }
    
    // Count passed and failed subjects
    let passedCount = 0;
    let failedCount = 0;
    
    subjects.forEach(subject => {
        if (subject.passed) {
            passedCount++;
        } else {
            failedCount++;
        }
    });
    
    const labels = [];
    const dataPoints = [];
    
    if (passedCount > 0) {
        labels.push('Passed');
        dataPoints.push(passedCount);
    }
    
    if (failedCount > 0) {
        labels.push('Failed');
        dataPoints.push(failedCount);
    }
    
    if (dataPoints.length === 0) {
        ctx.parentNode.innerHTML = '<p style="text-align: center; padding: 20px; color: #666;">No pass/fail data available</p>';
        return;
    }
    
    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: labels,
            datasets: [{
                data: dataPoints,
                backgroundColor: [
                    'rgba(40, 167, 69, 0.8)',   // Green for passed
                    'rgba(220, 53, 69, 0.8)'     // Red for failed
                ],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: `Pass/Fail Overview (${subjects.length} Subjects)`,
                    font: { size: 14, weight: 'bold' },
                    padding: { top: 10, bottom: 15 }
                },
                legend: {
                    position: 'bottom',
                    labels: {
                        boxWidth: 15,
                        padding: 10,
                        font: { size: 11 },
                        generateLabels: function(chart) {
                            const data = chart.data;
                            if (data.labels.length && data.datasets.length) {
                                return data.labels.map((label, i) => {
                                    const value = data.datasets[0].data[i];
                                    return {
                                        text: `${label}: ${value} subject${value !== 1 ? 's' : ''}`,
                                        fillStyle: data.datasets[0].backgroundColor[i],
                                        hidden: false,
                                        index: i
                                    };
                                });
                            }
                            return [];
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Download PDF of results
 */
function downloadPDF(launchId, studentId) {
    window.location.href = `/api/download-pdf/${launchId}/${studentId}`;
}
