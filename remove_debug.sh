#!/bin/bash
# Script to remove all debug statements from AnalyzerDAO.java

file="d:/AVTIVE PROJ/AcademicAnalyzer/src/com/sms/dao/AnalyzerDAO.java"

# Remove lines with debug patterns
sed -i '/System\.out\.println.*@@@/d' "$file"
sed -i '/System\.out\.println.*!!!/d' "$file" 
sed -i '/System\.out\.println.*\$\$\$/d' "$file"
sed -i '/System\.out\.println.*##########/d' "$file"
sed -i '/System\.out\.println.*\*\*\*\*\*\*\*\*\*\*/d' "$file"
sed -i '/System\.out\.println.*===/d' "$file"
sed -i '/System\.out\.println.*\+\+\+/d' "$file"
sed -i '/System\.out\.println.*RESULT:/d' "$file"
sed -i '/System\.out\.println.*DEBUG/d' "$file"
sed -i '/System\.out\.println.*Component passed:/d' "$file"
sed -i '/System\.out\.println.*Contribution:/d' "$file"
sed -i '/System\.out\.println.*Total:/d' "$file"
sed -i '/System\.out\.println.*All components passed:/d' "$file"
sed -i '/System\.out\.println.*Will mark as:/d' "$file"
sed -i '/System\.out\.println.*FINAL percentage after fail check:/d' "$file"
sed -i '/System\.out\.println.*Subject totals map entries:/d' "$file"
sed -i '/System\.out\.println.*Total subjects:/d' "$file"
sed -i '/System\.out\.println.*Failed subjects count:/d' "$file"
sed -i '/System\.out\.println.*Calculated percentage:/d' "$file"
sed -i '/System\.out\.println.*Components checked:/d' "$file"

echo "Debug statements removed from $file"