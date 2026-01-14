# Library Section - Technical Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [UI Components](#ui-components)
4. [Core Functionality](#core-functionality)
5. [Database Integration](#database-integration)
6. [Methods Reference](#methods-reference)
7. [Section Management](#section-management)
8. [Hierarchical Organization](#hierarchical-organization)
9. [Integration Points](#integration-points)
10. [Error Handling](#error-handling)

---

## 1. Overview

### Purpose
The **Library Section** is a centralized hub for managing and organizing all academic sections in the system. It provides a hierarchical view of sections organized by academic year and semester, with quick access to section rankings, editing, and deletion capabilities.

### Key Features
- **Hierarchical Organization**: Sections grouped by Academic Year → Semester
- **Collapsible Semesters**: Expand/collapse semesters to manage view
- **Section Cards**: Interactive cards displaying section name and student count
- **Quick Actions**: Click to view ranking, right-click for edit/delete
- **Auto-Assignment**: Sections without year/semester auto-assigned to 2025 Semester 1
- **Empty State**: User-friendly message when no sections exist
- **Real-time Updates**: Refreshes when sections are modified

### Technology Stack
- **Language**: Java 8+
- **UI Framework**: Java Swing
- **Layout Managers**: BoxLayout, BorderLayout, GridBagLayout
- **Design Pattern**: MVC with Observer pattern for updates
- **Database Access**: DAO pattern (SectionDAO, SectionEditDAO, StudentDAO)

---

## 2. Architecture

### Class Structure

```
YearSemesterPanel.java (Main Component)
├── Extends: JPanel
├── Dependencies:
│   ├── SectionDAO (Data Access)
│   ├── SectionEditDAO (Edit/Delete Operations)
│   ├── StudentDAO (Student Count)
│   ├── SectionCardPanel (Card UI)
│   ├── SectionAnalyzer (Ranking View)
│   └── DashboardScreen (Parent Container)
└── Responsibilities:
    ├── Hierarchical display of sections
    ├── Year/Semester grouping
    ├── Section card creation
    ├── Context menu management
    ├── Click handler registration
    └── Data refresh coordination
```

### Component Hierarchy

```
DashboardScreen (JFrame)
└── CardLayout (View Switcher)
    ├── Dashboard View
    ├── Library View
    │   └── JScrollPane
    │       └── YearSemesterPanel
    │           ├── Year Panel 1 (2025)
    │           │   ├── Semester 1 Panel
    │           │   │   └── Section Cards Container
    │           │   │       ├── SectionCardPanel (A ISE)
    │           │   │       ├── SectionCardPanel (B)
    │           │   │       └── ...
    │           │   └── Semester 2 Panel
    │           │       └── Section Cards Container
    │           ├── Year Panel 2 (2024)
    │           └── Unassigned Sections Panel
    └── Other Views (Create Section, Student Entry, etc.)
```

---

## 3. UI Components

### 3.1 Library Main Layout

**Structure:**
```
┌─────────────────────────────────────────────────────────┐
│  Section Library              [← Back to Dashboard]     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  📅 Academic Year 2025                                  │
│  ┌────────────────────────────────────────────────────┐│
│  │  ▼ Semester 1 (5 sections)                         ││
│  │                                                      ││
│  │  [A ISE]  [B]  [C]  [D ISE]  [E]                   ││
│  │  50 Students  48 Students  52 Students...           ││
│  │                                                      ││
│  │  ▼ Semester 2 (3 sections)                         ││
│  │                                                      ││
│  │  [A ISE]  [B]  [C]                                 ││
│  └────────────────────────────────────────────────────┘│
│                                                          │
│  📅 Academic Year 2024                                  │
│  ┌────────────────────────────────────────────────────┐│
│  │  ▼ Semester 1 (4 sections)                         ││
│  │                                                      ││
│  │  [A]  [B]  [C ISE]  [D]                            ││
│  └────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

**Color Scheme:**
```java
BACKGROUND_COLOR = #F8FAFCF (Light blue-gray)
CARD_BACKGROUND = #FFFFFF (White)
TEXT_PRIMARY = #1F2937 (Dark gray)
TEXT_SECONDARY = #6B7280 (Medium gray)
BORDER_COLOR = #E5E7EB (Light gray)
ACCENT_COLOR = #6366F1 (Indigo)
```

---

### 3.2 Year Panel

**Purpose**: Container for all semesters in an academic year

**Layout:**
```
┌───────────────────────────────────────────────┐
│  📅 Academic Year 2025                        │
│                                                │
│  [Semester 1 Panel]                           │
│                                                │
│  [Semester 2 Panel]                           │
└───────────────────────────────────────────────┘
```

**Styling:**
- Background: White (#FFFFFF)
- Border: 1px solid light gray
- Padding: 15px all sides
- Font: SansSerif Bold 18pt for year header
- Icon: 📅 (Calendar emoji)

**Code:**
```java
private JPanel createYearPanel(int year, Map<Integer, List<SectionInfo>> semesterMap) {
    JPanel yearPanel = new JPanel();
    yearPanel.setBackground(CARD_BACKGROUND);
    yearPanel.setLayout(new BoxLayout(yearPanel, BoxLayout.Y_AXIS));
    yearPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(BORDER_COLOR, 1),
        new EmptyBorder(15, 15, 15, 15)
    ));
    
    // Year header
    JLabel yearLabel = new JLabel("📅 Academic Year " + year);
    yearLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
    yearLabel.setForeground(TEXT_PRIMARY);
    
    // Add semesters
    List<Integer> semesters = new ArrayList<>(semesterMap.keySet());
    Collections.sort(semesters);
    
    for (Integer semester : semesters) {
        if (semester == 0) continue;
        JPanel semesterPanel = createSemesterPanel(semester, semesterMap.get(semester));
        yearPanel.add(semesterPanel);
        yearPanel.add(Box.createVerticalStrut(10));
    }
    
    return yearPanel;
}
```

---

### 3.3 Semester Panel (Collapsible)

**Purpose**: Container for all sections in a semester with expand/collapse functionality

**Layout (Expanded):**
```
┌──────────────────────────────────────────────┐
│  ▼ Semester 1 (5 sections)                   │
│                                               │
│  [Section Card] [Section Card] [Section Card]│
└──────────────────────────────────────────────┘
```

**Layout (Collapsed):**
```
┌──────────────────────────────────────────────┐
│  ► Semester 1 (5 sections)                   │
└──────────────────────────────────────────────┘
```

**Features:**
- Clickable header to toggle visibility
- Arrow indicator (▼ = expanded, ► = collapsed)
- Shows section count in header
- Smooth collapse/expand animation
- Default state: Expanded

**Code:**
```java
private JPanel createSemesterPanel(int semester, List<SectionInfo> sections) {
    JPanel semesterPanel = new JPanel();
    semesterPanel.setBackground(BACKGROUND_COLOR);
    semesterPanel.setLayout(new BoxLayout(semesterPanel, BoxLayout.Y_AXIS));
    
    // Collapsible semester header
    JPanel semesterHeader = new JPanel(new BorderLayout());
    semesterHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
    
    JLabel semesterLabel = new JLabel(
        "▼ Semester " + semester + " (" + sections.size() + " sections)"
    );
    semesterLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
    semesterLabel.setForeground(ACCENT_COLOR);
    
    // Sections container
    JPanel sectionsContainer = new JPanel();
    sectionsContainer.setLayout(new BoxLayout(sectionsContainer, BoxLayout.X_AXIS));
    
    // Add section cards
    for (SectionInfo section : sections) {
        SectionCardPanel card = new SectionCardPanel(
            section.sectionName, 
            section.totalStudents
        );
        addContextMenuToCard(card, section);
        addClickHandlerToCard(card, section);
        sectionsContainer.add(card);
        sectionsContainer.add(Box.createHorizontalStrut(15));
    }
    
    // Add expand/collapse functionality
    final boolean[] isExpanded = {true};
    semesterHeader.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            isExpanded[0] = !isExpanded[0];
            sectionsContainer.setVisible(isExpanded[0]);
            semesterLabel.setText(
                (isExpanded[0] ? "▼" : "►") + 
                " Semester " + semester + 
                " (" + sections.size() + " sections)"
            );
            semesterPanel.revalidate();
            semesterPanel.repaint();
        }
    });
    
    semesterPanel.add(semesterHeader);
    semesterPanel.add(Box.createVerticalStrut(10));
    semesterPanel.add(sectionsContainer);
    
    return semesterPanel;
}
```

---

### 3.4 Section Card Panel

**Purpose**: Display individual section with name and student count

**Layout:**
```
┌─────────────┐
│   A ISE     │ (Section Name)
│     50      │ (Student Count)
│  Students   │
└─────────────┘
```

**Features:**
- Fixed size: 140px × 65px
- Rounded corners with shadow
- Hover effect (blue border highlight)
- Click to open ranking table
- Right-click for context menu
- Hand cursor on hover

**Styling:**
- Background: White with gradient
- Border: Rounded 8px, light gray (blue on hover)
- Shadow: Drop shadow effect
- Font: SansSerif Bold 12pt for name, Bold 20pt for count

**States:**
1. **Default**: White background, gray border
2. **Hover**: Blue border (accent color)
3. **Clicked**: Opens ranking table or context menu

**Code Structure:**
```java
public class SectionCardPanel extends JPanel {
    private String sectionName;
    private int studentCount;
    private boolean isHovered = false;
    
    public SectionCardPanel(String sectionName, int studentCount) {
        setPreferredSize(new Dimension(140, 65));
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Section name label
        JLabel sectionLabel = new JLabel(sectionName);
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        sectionLabel.setForeground(new Color(31, 41, 55));
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Student count label
        JLabel countLabel = new JLabel(String.valueOf(studentCount));
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        countLabel.setForeground(new Color(31, 41, 55));
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // "Students" label
        JLabel studentsLabel = new JLabel("Students");
        studentsLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        studentsLabel.setForeground(new Color(107, 114, 128));
        studentsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(sectionLabel);
        contentPanel.add(Box.createVerticalStrut(1));
        contentPanel.add(countLabel);
        contentPanel.add(Box.createVerticalStrut(0));
        contentPanel.add(studentsLabel);
        contentPanel.add(Box.createVerticalGlue());
        
        add(contentPanel);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw rounded rectangle with shadow
        if (isHovered) {
            g2.setColor(new Color(99, 102, 241, 30)); // Blue highlight
        } else {
            g2.setColor(Color.WHITE);
        }
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        
        // Border
        g2.setColor(isHovered ? new Color(99, 102, 241) : new Color(229, 231, 235));
        g2.setStroke(new BasicStroke(isHovered ? 2 : 1));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        
        g2.dispose();
    }
}
```

---

### 3.5 Context Menu

**Purpose**: Right-click menu for section management

**Menu Items:**
1. **✏️ Edit Section**: Opens dialog to rename section
2. **🗑️ Delete Section**: Confirms and deletes section with all data

**Layout:**
```
┌─────────────────────┐
│ ✏️ Edit Section     │
├─────────────────────┤
│ 🗑️ Delete Section  │ (Red text)
└─────────────────────┘
```

**Trigger**: Right-click on section card

**Code:**
```java
private void addContextMenuToCard(SectionCardPanel card, SectionInfo section) {
    JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.setBackground(CARD_BACKGROUND);
    
    // Edit menu item
    JMenuItem editItem = new JMenuItem("✏️ Edit Section");
    editItem.setFont(new Font("SansSerif", Font.PLAIN, 13));
    editItem.addActionListener(e -> showEditDialog(section));
    popupMenu.add(editItem);
    
    popupMenu.addSeparator();
    
    // Delete menu item
    JMenuItem deleteItem = new JMenuItem("🗑️ Delete Section");
    deleteItem.setFont(new Font("SansSerif", Font.PLAIN, 13));
    deleteItem.setForeground(new Color(220, 38, 38)); // Red
    deleteItem.addActionListener(e -> confirmAndDeleteSection(section));
    popupMenu.add(deleteItem);
    
    // Add right-click listener
    card.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu.show(card, e.getX(), e.getY());
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu.show(card, e.getX(), e.getY());
            }
        }
    });
}
```

---

### 3.6 Empty State

**Purpose**: Display when no sections exist

**Layout:**
```
┌─────────────────────────────────┐
│                                  │
│            📚                   │
│                                  │
│      No Sections Yet            │
│                                  │
│  Create your first section      │
│        to get started            │
│                                  │
└─────────────────────────────────┘
```

**Features:**
- Centered vertically and horizontally
- Large emoji icon (48pt)
- Bold title (20pt)
- Subtitle with instruction (14pt)
- Gray text color

**Code:**
```java
private JPanel createEmptyState() {
    JPanel emptyPanel = new JPanel();
    emptyPanel.setOpaque(false);
    emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
    emptyPanel.setBorder(new EmptyBorder(50, 50, 50, 50));
    
    JLabel emptyIcon = new JLabel("📚", JLabel.CENTER);
    emptyIcon.setFont(new Font("SansSerif", Font.PLAIN, 48));
    emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    JLabel emptyTitle = new JLabel("No Sections Yet");
    emptyTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
    emptyTitle.setForeground(TEXT_SECONDARY);
    emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    JLabel emptySubtitle = new JLabel("Create your first section to get started");
    emptySubtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
    emptySubtitle.setForeground(TEXT_SECONDARY);
    emptySubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    emptyPanel.add(emptyIcon);
    emptyPanel.add(Box.createVerticalStrut(15));
    emptyPanel.add(emptyTitle);
    emptyPanel.add(Box.createVerticalStrut(8));
    emptyPanel.add(emptySubtitle);
    
    return emptyPanel;
}
```

---

## 4. Core Functionality

### 4.1 Data Loading & Organization

**Flow Diagram:**
```
Application Start / Library Click
    ↓
refreshLibrary()
    ↓
Query sections from database (by userId)
    ↓
Auto-assign year/semester if missing (2025 Semester 1)
    ↓
Group sections by Year → Semester
    ↓
Sort years (descending) and semesters (ascending)
    ↓
Create UI panels for each year/semester
    ↓
Add section cards with click handlers
    ↓
Display in Library View
```

**Method Chain:**
```java
// 1. Trigger refresh
public void showLibrary() {
    refreshLibrary();
    cardLayout.show(mainContentPanel, LIBRARY_VIEW);
}

// 2. Load sections from database
private void refreshLibrary() {
    BackgroundTaskUtil.executeAsync(() -> {
        List<SectionInfo> sections = sectionService.getUserSections(userId);
        SwingUtilities.invokeLater(() -> {
            yearSemesterPanel.updateSections(sections);
        });
    });
}

// 3. Update UI with sections
public void updateSections(List<SectionInfo> sections) {
    removeAll();
    
    // Auto-assign year/semester
    for (SectionInfo section : sections) {
        if (section.academicYear == 0) {
            section.academicYear = 2025;
            section.semester = 1;
        }
    }
    
    // Group sections
    Map<Integer, Map<Integer, List<SectionInfo>>> yearSemesterMap = 
        groupSectionsByYearAndSemester(sections);
    
    // Create UI
    List<Integer> years = new ArrayList<>(yearSemesterMap.keySet());
    Collections.sort(years, Collections.reverseOrder());
    
    for (Integer year : years) {
        if (year == 0) continue;
        JPanel yearPanel = createYearPanel(year, yearSemesterMap.get(year));
        add(yearPanel);
        add(Box.createVerticalStrut(15));
    }
    
    revalidate();
    repaint();
}
```

---

### 4.2 Section Grouping Algorithm

**Purpose**: Organize sections into hierarchical structure Year → Semester → Sections

**Data Structure:**
```java
Map<Integer, Map<Integer, List<SectionInfo>>> yearSemesterMap
    Key: Year (Integer)
    Value: Map<Integer, List<SectionInfo>>
        Key: Semester (Integer)
        Value: List<SectionInfo> (sections in that semester)
```

**Example:**
```
{
    2025: {
        1: [A ISE, B, C, D ISE, E],
        2: [A ISE, B, C]
    },
    2024: {
        1: [A, B, C ISE, D],
        2: [A, B]
    },
    0: {  // Unassigned sections
        0: [Old Section 1, Old Section 2]
    }
}
```

**Implementation:**
```java
private Map<Integer, Map<Integer, List<SectionInfo>>> groupSectionsByYearAndSemester(
    List<SectionInfo> sections
) {
    Map<Integer, Map<Integer, List<SectionInfo>>> map = new HashMap<>();
    
    for (SectionInfo section : sections) {
        int year = section.academicYear;
        int semester = section.semester;
        
        // Create year map if not exists
        map.putIfAbsent(year, new HashMap<>());
        
        // Create semester list if not exists
        map.get(year).putIfAbsent(semester, new ArrayList<>());
        
        // Add section to semester list
        map.get(year).get(semester).add(section);
    }
    
    return map;
}
```

**Sorting Logic:**
1. **Years**: Descending order (2025, 2024, 2023, ...)
2. **Semesters**: Ascending order (1, 2, 3, ...)
3. **Sections**: Database order (by ID or name)

---

### 4.3 Click Handler Registration

**Purpose**: Register click and right-click handlers for section cards

**Handler Types:**
1. **Left Click**: Opens ranking table for section
2. **Right Click**: Shows context menu (edit/delete)
3. **Hover**: Visual feedback (blue border)

**Implementation:**
```java
private void addClickHandlerToCard(SectionCardPanel card, SectionInfo section) {
    System.out.println("@@@ ADDING CLICK HANDLER TO CARD: " + section.sectionName + " @@@");
    
    MouseAdapter clickListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            // Only trigger on left-click, not right-click
            if (e.getButton() == MouseEvent.BUTTON1) {
                System.out.println("@@@ OPENING SECTION RANKING FOR: " + section.sectionName + " @@@");
                openSectionRanking(section);
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            // Handle right-click for context menu - don't interfere
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                return; // Let the context menu handler deal with it
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            // For debugging
            System.out.println("@@@ MOUSE RELEASED: button=" + e.getButton() + " @@@");
        }
    };
    
    card.addMouseListener(clickListener);
    System.out.println("@@@ CLICK HANDLER ADDED TO CARD: " + section.sectionName + " @@@");
}
```

**Important Notes:**
- Must check button type to avoid conflict between left and right click
- MouseEvent.BUTTON1 = Left click
- MouseEvent.BUTTON3 = Right click
- isPopupTrigger() handles platform-specific right-click detection

---

### 4.4 Opening Section Ranking Table

**Purpose**: Navigate to section ranking table when card is clicked

**Flow:**
```
User clicks section card (left-click)
    ↓
openSectionRanking(section)
    ↓
Get parent DashboardScreen instance
    ↓
Call dashboard.showSectionRankingTable(sectionId, sectionName)
    ↓
DashboardScreen replaces main content with ranking table
    ↓
Sidebar stays visible, Back button returns to library
```

**Implementation:**
```java
private void openSectionRanking(SectionInfo section) {
    System.out.println("@@@ OPENING RANKING TABLE FOR: " + section.sectionName + " @@@");
    
    // Get the DashboardScreen instance
    Window window = SwingUtilities.getWindowAncestor(this);
    if (window instanceof com.sms.dashboard.DashboardScreen) {
        com.sms.dashboard.DashboardScreen dashboard = 
            (com.sms.dashboard.DashboardScreen) window;
        
        // Use DashboardScreen's method to show section ranking table
        // This will display just the table in the main content area 
        // while keeping sidebar visible
        dashboard.showSectionRankingTable(section.id, section.sectionName);
    } else {
        System.err.println("@@@ ERROR: Parent window is not DashboardScreen @@@");
    }
}
```

**DashboardScreen Integration:**
```java
// In DashboardScreen.java
public void showSectionRankingTable(int sectionId, String sectionName) {
    // Fetch students for section
    HashMap<String, ArrayList<Student>> sectionStudents = new HashMap<>();
    
    Connection conn = DatabaseConnection.getConnection();
    String query = 
        "SELECT s.id, s.name, s.roll_number " +
        "FROM students s " +
        "WHERE s.section_id = ? " +
        "ORDER BY s.roll_number";
    
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, sectionId);
    ResultSet rs = stmt.executeQuery();
    
    ArrayList<Student> students = new ArrayList<>();
    while (rs.next()) {
        Student student = new Student(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("roll_number"),
            sectionId
        );
        students.add(student);
    }
    
    sectionStudents.put(sectionName, students);
    
    // Create analyzer with embedded mode
    SectionAnalyzer analyzer = new SectionAnalyzer(
        this,
        sectionStudents,
        () -> showLibrary() // Back button returns to library
    );
    
    analyzer.currentSectionId = sectionId;
    analyzer.currentSectionName = sectionName;
    
    // Replace main panel (sidebar stays visible)
    mainPanel.removeAll();
    mainPanel.add(analyzer, BorderLayout.CENTER);
    mainPanel.revalidate();
    mainPanel.repaint();
}
```

---

## 5. Database Integration

### 5.1 SectionDAO

**Purpose**: Load section information from database

**Query:**
```sql
SELECT 
    s.id,
    s.section_name,
    s.academic_year,
    s.semester,
    s.user_id,
    COUNT(st.id) as total_students
FROM sections s
LEFT JOIN students st ON s.id = st.section_id
WHERE s.user_id = ?
GROUP BY s.id, s.section_name, s.academic_year, s.semester, s.user_id
ORDER BY s.academic_year DESC, s.semester ASC, s.section_name ASC
```

**Data Model:**
```java
public static class SectionInfo {
    public int id;
    public String sectionName;
    public int academicYear;
    public int semester;
    public int userId;
    public int totalStudents;
}
```

---

### 5.2 SectionEditDAO

**Purpose**: Edit and delete sections

**Update Section Name:**
```java
public boolean updateSectionName(int sectionId, String newName, int userId) {
    String query = 
        "UPDATE sections " +
        "SET section_name = ? " +
        "WHERE id = ? AND user_id = ?";
    
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setString(1, newName);
    stmt.setInt(2, sectionId);
    stmt.setInt(3, userId);
    
    int rowsAffected = stmt.executeUpdate();
    return rowsAffected > 0;
}
```

**Delete Section (Cascade):**
```java
public boolean deleteSection(int sectionId, int userId) {
    // Delete in order to maintain referential integrity
    
    // 1. Delete marks
    String deleteMarks = 
        "DELETE FROM marks WHERE section_id = ?";
    stmt = conn.prepareStatement(deleteMarks);
    stmt.setInt(1, sectionId);
    stmt.executeUpdate();
    
    // 2. Delete students
    String deleteStudents = 
        "DELETE FROM students WHERE section_id = ?";
    stmt = conn.prepareStatement(deleteStudents);
    stmt.setInt(1, sectionId);
    stmt.executeUpdate();
    
    // 3. Delete section
    String deleteSection = 
        "DELETE FROM sections WHERE id = ? AND user_id = ?";
    stmt = conn.prepareStatement(deleteSection);
    stmt.setInt(1, sectionId);
    stmt.setInt(2, userId);
    
    int rowsAffected = stmt.executeUpdate();
    return rowsAffected > 0;
}
```

---

### 5.3 StudentDAO

**Purpose**: Fetch students for ranking table

**Query:**
```sql
SELECT 
    s.id,
    s.name,
    s.roll_number
FROM students s
WHERE s.section_id = ?
ORDER BY s.roll_number ASC
```

**Used By**: `DashboardScreen.showSectionRankingTable()`

---

## 6. Methods Reference

### 6.1 Constructor

```java
public YearSemesterPanel(int userId, Runnable refreshCallback)
```

**Purpose:** Initialize library panel with user context and refresh callback

**Parameters:**
- `userId` - Current user ID for filtering sections
- `refreshCallback` - Callback to refresh library when sections change

**Usage:**
```java
YearSemesterPanel yearSemesterPanel = new YearSemesterPanel(
    userId,
    this::refreshLibrary
);
```

---

### 6.2 Data Loading Methods

#### updateSections()
```java
public void updateSections(List<SectionInfo> sections)
```

**Purpose:** Update panel with new section data

**Process:**
1. Clear existing UI
2. Handle empty state if no sections
3. Auto-assign year/semester to sections without them
4. Group sections by year and semester
5. Create UI for each year
6. Revalidate and repaint

**Parameters:**
- `sections` - List of sections to display

---

#### groupSectionsByYearAndSemester()
```java
private Map<Integer, Map<Integer, List<SectionInfo>>> groupSectionsByYearAndSemester(
    List<SectionInfo> sections
)
```

**Purpose:** Group sections into hierarchical map

**Returns:** `Map<Year, Map<Semester, List<SectionInfo>>>`

**Algorithm:**
1. Create nested map structure
2. Iterate through sections
3. Add each section to appropriate year/semester list
4. Return grouped map

---

### 6.3 UI Creation Methods

#### createYearPanel()
```java
private JPanel createYearPanel(int year, Map<Integer, List<SectionInfo>> semesterMap)
```

**Purpose:** Create panel for academic year with all semesters

**Parameters:**
- `year` - Academic year (e.g., 2025)
- `semesterMap` - Map of semester → sections

**Returns:** JPanel with year header and semester panels

---

#### createSemesterPanel()
```java
private JPanel createSemesterPanel(int semester, List<SectionInfo> sections)
```

**Purpose:** Create collapsible panel for semester with section cards

**Parameters:**
- `semester` - Semester number (1, 2, 3, etc.)
- `sections` - List of sections in semester

**Returns:** JPanel with semester header and section cards container

**Features:**
- Collapsible header with click handler
- Arrow indicator (▼/►)
- Section count display
- Horizontal section cards layout

---

#### createUnassignedPanel()
```java
private JPanel createUnassignedPanel(Map<Integer, List<SectionInfo>> semesterMap)
```

**Purpose:** Create panel for sections without year/semester assignment

**Parameters:**
- `semesterMap` - Map of unassigned sections

**Returns:** JPanel with "Other Sections" header and cards

---

#### createEmptyState()
```java
private JPanel createEmptyState()
```

**Purpose:** Create empty state UI when no sections exist

**Returns:** JPanel with centered icon and message

---

### 6.4 Handler Registration Methods

#### addContextMenuToCard()
```java
private void addContextMenuToCard(SectionCardPanel card, SectionInfo section)
```

**Purpose:** Add right-click context menu to section card

**Parameters:**
- `card` - Section card panel
- `section` - Section info for menu actions

**Menu Items:**
- Edit Section
- Delete Section

---

#### addClickHandlerToCard()
```java
private void addClickHandlerToCard(SectionCardPanel card, SectionInfo section)
```

**Purpose:** Add left-click handler to open ranking table

**Parameters:**
- `card` - Section card panel
- `section` - Section info for navigation

**Behavior:**
- Left click → Opens ranking table
- Right click → Opens context menu (handled separately)

---

### 6.5 Action Methods

#### openSectionRanking()
```java
private void openSectionRanking(SectionInfo section)
```

**Purpose:** Navigate to section ranking table

**Parameters:**
- `section` - Section to display ranking for

**Process:**
1. Get parent DashboardScreen instance
2. Call `dashboard.showSectionRankingTable()`
3. Display ranking in main content area

---

#### showEditDialog()
```java
private void showEditDialog(SectionInfo section)
```

**Purpose:** Show dialog to edit section name

**Parameters:**
- `section` - Section to edit

**Process:**
1. Show input dialog with current name
2. Validate new name
3. Update database via SectionEditDAO
4. Show success/error message
5. Refresh library if successful

---

#### confirmAndDeleteSection()
```java
private void confirmAndDeleteSection(SectionInfo section)
```

**Purpose:** Confirm and delete section with all data

**Parameters:**
- `section` - Section to delete

**Process:**
1. Show confirmation dialog with warning
2. If confirmed, delete via SectionEditDAO
3. Show success/error message
4. Refresh library if successful

**Warning Message:**
```
Are you sure you want to delete section 'A ISE'?

This will permanently delete:
• All students in this section
• All marks and grades
• All associated data

This action cannot be undone!
```

---

## 7. Section Management

### 7.1 Edit Section Name

**Flow:**
```
Right-click on section card
    ↓
Select "✏️ Edit Section"
    ↓
Show input dialog with current name
    ↓
User enters new name
    ↓
Validate name (not empty, different from current)
    ↓
Update database (SectionEditDAO.updateSectionName())
    ↓
Show success message
    ↓
Refresh library (refreshCallback.run())
```

**Code:**
```java
private void showEditDialog(SectionInfo section) {
    String newName = JOptionPane.showInputDialog(
        this,
        "Enter new section name:",
        section.sectionName
    );
    
    if (newName != null && !newName.trim().isEmpty() && !newName.equals(section.sectionName)) {
        SectionEditDAO editDAO = new SectionEditDAO();
        if (editDAO.updateSectionName(section.id, newName.trim(), userId)) {
            JOptionPane.showMessageDialog(
                this,
                "Section name updated successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
            if (refreshCallback != null) {
                refreshCallback.run();
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to update section name.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
```

---

### 7.2 Delete Section

**Flow:**
```
Right-click on section card
    ↓
Select "🗑️ Delete Section"
    ↓
Show confirmation dialog with warning
    ↓
User confirms deletion
    ↓
Delete from database (cascade: marks → students → section)
    ↓
Show success message
    ↓
Refresh library (refreshCallback.run())
```

**Deletion Order (Referential Integrity):**
1. Delete marks (marks.section_id FK)
2. Delete students (students.section_id FK)
3. Delete section (sections.id PK)

**Code:**
```java
private void confirmAndDeleteSection(SectionInfo section) {
    int result = JOptionPane.showConfirmDialog(
        this,
        "Are you sure you want to delete section '" + section.sectionName + "'?\n\n" +
        "This will permanently delete:\n" +
        "• All students in this section\n" +
        "• All marks and grades\n" +
        "• All associated data\n\n" +
        "This action cannot be undone!",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );
    
    if (result == JOptionPane.YES_OPTION) {
        SectionEditDAO editDAO = new SectionEditDAO();
        if (editDAO.deleteSection(section.id, userId)) {
            JOptionPane.showMessageDialog(
                this,
                "Section deleted successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
            if (refreshCallback != null) {
                refreshCallback.run();
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to delete section. Please try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
```

---

## 8. Hierarchical Organization

### 8.1 Auto-Assignment Logic

**Purpose**: Assign default year/semester to sections without them

**Default Values:**
- Academic Year: 2025
- Semester: 1

**Implementation:**
```java
public void updateSections(List<SectionInfo> sections) {
    // Auto-assign year 2025 semester 1 to sections without year/semester
    for (SectionInfo section : sections) {
        if (section.academicYear == 0) {
            section.academicYear = 2025;
            section.semester = 1;
        }
    }
    
    // Continue with grouping...
}
```

**Rationale:**
- Ensures all sections appear in organized view
- Prevents sections from being lost in "Unassigned" category
- User can edit year/semester later if needed

---

### 8.2 Sorting Rules

**Year Sorting**: Descending (Most recent first)
```java
Collections.sort(years, Collections.reverseOrder());
// Result: [2025, 2024, 2023, ...]
```

**Semester Sorting**: Ascending (Semester 1 first)
```java
Collections.sort(semesters);
// Result: [1, 2, 3, ...]
```

**Section Sorting**: Database order (typically by ID or name)

---

### 8.3 Display Order

**Final Display Structure:**
```
1. Academic Year 2025 (most recent)
   - Semester 1
     - Sections [A ISE, B, C, ...]
   - Semester 2
     - Sections [...]

2. Academic Year 2024
   - Semester 1
     - Sections [...]
   - Semester 2
     - Sections [...]

3. Other Sections (Unassigned, if any)
   - Sections without year/semester
```

---

## 9. Integration Points

### 9.1 Dashboard Integration

**Opening Library from Dashboard:**
```java
// In DashboardScreen.java
sidebarPanel.setLibraryClickListener(e -> showLibrary());

public void showLibrary() {
    refreshLibrary(); // Load latest data
    cardLayout.show(mainContentPanel, LIBRARY_VIEW);
}
```

**Returning to Dashboard:**
```java
// Back button in library panel
JButton backBtn = new JButton("← Back to Dashboard");
backBtn.addActionListener(e -> showDashboard());

public void showDashboard() {
    cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
}
```

---

### 9.2 Section Analyzer Integration

**Opening Ranking Table:**
```java
// When section card clicked
dashboard.showSectionRankingTable(sectionId, sectionName);

// In DashboardScreen.java
public void showSectionRankingTable(int sectionId, String sectionName) {
    // Fetch students
    HashMap<String, ArrayList<Student>> sectionStudents = 
        fetchStudentsForSection(sectionId);
    
    // Create Section Analyzer
    SectionAnalyzer analyzer = new SectionAnalyzer(
        this,
        sectionStudents,
        () -> showLibrary() // Back button callback
    );
    
    analyzer.currentSectionId = sectionId;
    analyzer.currentSectionName = sectionName;
    
    // Replace main panel
    mainPanel.removeAll();
    mainPanel.add(analyzer, BorderLayout.CENTER);
    mainPanel.revalidate();
    mainPanel.repaint();
}
```

**Returning to Library:**
```java
// Back button in Section Analyzer
JButton backButton = new JButton("← Back to Library");
backButton.addActionListener(e -> {
    if (onBackCallback != null) {
        onBackCallback.run(); // Calls dashboard.showLibrary()
    }
});
```

---

### 9.3 Refresh Mechanism

**Callback Pattern:**
```java
// When Library is created
yearSemesterPanel = new YearSemesterPanel(userId, this::refreshLibrary);

// When section is modified
if (refreshCallback != null) {
    refreshCallback.run(); // Triggers refreshLibrary()
}

// Refresh implementation
private void refreshLibrary() {
    BackgroundTaskUtil.executeAsync(() -> {
        List<SectionInfo> sections = sectionService.getUserSections(userId);
        SwingUtilities.invokeLater(() -> {
            yearSemesterPanel.updateSections(sections);
        });
    });
}
```

---

## 10. Error Handling

### 10.1 Database Errors

**Connection Handling:**
```java
try {
    Connection conn = DatabaseConnection.getConnection();
    if (conn == null || conn.isClosed()) {
        throw new SQLException("Database connection not available");
    }
    
    // Execute queries
    
} catch (SQLException e) {
    JOptionPane.showMessageDialog(
        this,
        "Database error: " + e.getMessage(),
        "Error",
        JOptionPane.ERROR_MESSAGE
    );
    e.printStackTrace();
}
```

---

### 10.2 Empty Data Handling

**No Sections:**
```java
if (sections == null || sections.isEmpty()) {
    add(createEmptyState());
    revalidate();
    repaint();
    return;
}
```

**No Students in Section:**
```java
if (students.isEmpty()) {
    JOptionPane.showMessageDialog(
        this,
        "No students found in this section.",
        "No Students",
        JOptionPane.INFORMATION_MESSAGE
    );
    return;
}
```

---

### 10.3 Validation

**Section Name Validation:**
```java
if (newName != null && !newName.trim().isEmpty() && !newName.equals(section.sectionName)) {
    // Valid - proceed with update
} else {
    // Invalid - show error or do nothing
}
```

**Delete Confirmation:**
```java
int result = JOptionPane.showConfirmDialog(
    this,
    "Are you sure you want to delete section '" + section.sectionName + "'?...",
    "Confirm Delete",
    JOptionPane.YES_NO_OPTION,
    JOptionPane.WARNING_MESSAGE
);

if (result == JOptionPane.YES_OPTION) {
    // User confirmed - proceed with deletion
}
```

---

## 11. Conclusion

The **Library Section** provides a comprehensive, user-friendly interface for managing academic sections with the following key features:

✅ **Hierarchical Organization**: Year → Semester → Sections structure  
✅ **Interactive Cards**: Click to view ranking, right-click for edit/delete  
✅ **Collapsible Semesters**: Manage view complexity  
✅ **Auto-Assignment**: Sections without year/semester auto-assigned to default  
✅ **Real-time Updates**: Refreshes after edit/delete operations  
✅ **Empty State**: Clear messaging when no sections exist  
✅ **Integration**: Seamless navigation to Section Analyzer ranking tables  
✅ **Error Handling**: Robust validation and error messaging  

**Total Lines of Code**: 410 lines (YearSemesterPanel.java)  
**Key Dependencies**: SectionDAO, SectionEditDAO, StudentDAO, SectionCardPanel, SectionAnalyzer  
**UI Framework**: Java Swing with BoxLayout  
**Design Pattern**: MVC with Observer pattern for refresh callbacks  

---

**Document Version**: 1.0  
**Last Updated**: January 14, 2026  
**Author**: Academic Analyzer Development Team
