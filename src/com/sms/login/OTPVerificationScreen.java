package com.sms.login;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;

public class OTPVerificationScreen extends JFrame {
    
    // Reuse custom components
    static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private int shadowSize = 5;
        
        public RoundedPanel(int radius) {
            this.cornerRadius = radius;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (int i = 0; i < shadowSize; i++) {
                g2.setColor(new Color(0, 0, 0, 20 - (i * 4)));
                g2.fill(new RoundRectangle2D.Float(i, i, getWidth() - (i * 2), getHeight() - (i * 2), cornerRadius, cornerRadius));
            }
            
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(shadowSize, shadowSize, getWidth() - (shadowSize * 2), getHeight() - (shadowSize * 2), cornerRadius, cornerRadius));
        }
    }
    
    static class RoundedTextField extends JTextField {
        private int radius;
        
        public RoundedTextField(int radius) {
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            setFont(new Font("Arial", Font.PLAIN, 15));
            setBackground(new Color(248, 248, 248));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(new Color(248, 248, 248));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            
            super.paintComponent(g);
        }
    }
    
    static class RoundedButton extends JButton {
        private int radius;
        private Color backgroundColor;
        private Color hoverColor;
        private boolean isHovered = false;
        
        public RoundedButton(String text, int radius, Color bgColor, Color hoverColor) {
            super(text);
            this.radius = radius;
            this.backgroundColor = bgColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(isHovered ? hoverColor : backgroundColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
        }
    }
    
    private JTextField[] otpFields;
    private String email;
    private String correctOTP;

    public OTPVerificationScreen(String email, String correctOTP) {
        this.email = email;
        this.correctOTP = correctOTP;
        
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("OTP Verification - Academic Analyzer");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main container with gradient background
        JPanel mainContainer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(200, 190, 230),
                    getWidth(), getHeight(), new Color(150, 180, 220)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        // White card panel
        RoundedPanel cardPanel = new RoundedPanel(30);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(new Dimension(420, 400));
        
        // Inner content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Title
        JLabel titleLabel = new JLabel("Enter OTP");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(30, 30, 30));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Description
        JLabel descLabel = new JLabel("<html><center>We've sent a 6-digit OTP to<br>" + email + "</center></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(new Color(100, 100, 100));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // OTP input fields
        JPanel otpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        otpPanel.setOpaque(false);
        otpPanel.setMaximumSize(new Dimension(340, 60));
        otpPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        otpFields = new JTextField[6];
        for (int i = 0; i < 6; i++) {
            otpFields[i] = new RoundedTextField(10);
            otpFields[i].setPreferredSize(new Dimension(45, 50));
            otpFields[i].setHorizontalAlignment(JTextField.CENTER);
            otpFields[i].setDocument(new javax.swing.text.PlainDocument() {
                @Override
                public void insertString(int offs, String str, javax.swing.text.AttributeSet a) 
                    throws javax.swing.text.BadLocationException {
                    if (str != null && str.matches("\\d") && getLength() < 1) {
                        super.insertString(offs, str, a);
                        // Auto-focus next field
                        for (int j = 0; j < otpFields.length - 1; j++) {
                            if (otpFields[j].hasFocus() && otpFields[j].getText().length() == 1) {
                                otpFields[j + 1].requestFocus();
                                break;
                            }
                        }
                    }
                }
            });
            
            // Add key listener for backspace
            final int index = i;
            otpFields[i].addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && 
                        otpFields[index].getText().isEmpty() && index > 0) {
                        otpFields[index - 1].requestFocus();
                    }
                }
            });
            
            otpPanel.add(otpFields[i]);
        }
        
        // Verify button
        RoundedButton verifyButton = new RoundedButton("Verify OTP", 25, 
            new Color(100, 120, 200), new Color(80, 100, 180));
        verifyButton.setFont(new Font("Arial", Font.BOLD, 16));
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setMaximumSize(new Dimension(340, 50));
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Resend OTP link
        JLabel resendLabel = new JLabel("Resend OTP");
        resendLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        resendLabel.setForeground(new Color(100, 120, 200));
        resendLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resendLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        resendLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                JOptionPane.showMessageDialog(OTPVerificationScreen.this, 
                    "OTP resent to " + email, 
                    "OTP Resent", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                resendLabel.setText("<html><u>Resend OTP</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                resendLabel.setText("Resend OTP");
            }
        });
        
        // Add components
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(otpPanel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(verifyButton);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(resendLabel);
        
        cardPanel.add(contentPanel);
        mainContainer.add(cardPanel);
        
        // Verify action
        verifyButton.addActionListener(e -> verifyOTP());
        
        add(mainContainer);
        setVisible(true);
        
        // Focus first field
        otpFields[0].requestFocus();
    }
    
    private void verifyOTP() {
        StringBuilder enteredOTP = new StringBuilder();
        for (JTextField field : otpFields) {
            enteredOTP.append(field.getText());
        }
        
        if (enteredOTP.length() < 6) {
            JOptionPane.showMessageDialog(this, 
                "Please enter complete OTP", 
                "Incomplete OTP", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (enteredOTP.toString().equals(correctOTP)) {
            dispose();
            new ResetPasswordScreen(email);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid OTP. Please try again.", 
                "Invalid OTP", 
                JOptionPane.ERROR_MESSAGE);
            // Clear fields
            for (JTextField field : otpFields) {
                field.setText("");
            }
            otpFields[0].requestFocus();
        }
    }
}