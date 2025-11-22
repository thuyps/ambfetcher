#cd C:\amidatabase
@echo off
echo if port is occupiring:
echo get processID: netstat -ano | findstr : 2610
echo kill process: taskkill /PID processID /F
echo Start server
java -cp csvprovider.jar com.csv.CsvProviderActivity