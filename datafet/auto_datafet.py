import time
import schedule
from datetime import datetime
from pywinauto import Application, timings
import psutil
import subprocess
import os
from pywinauto import Desktop
import threading

# === THÔNG SỐ CẤU HÌNH ===
WINDOW_TITLE = " Datafet Pro v9.6"#"Datafet Pro v9.6"                      # Tiêu đề cửa sổ của tool
APP_PATH = r"C:\DatafetApp\DatafetApp\Datafet.exe"                # Đường dẫn đến file .exe của tool
SCHEDULE_TIME1 = "08:00"
SCHEDULE_TIME2 = "12:00"
SCHEDULE_TIME3 = "16:00"
SCHEDULE_TIME4 = "20:00"
SCHEDULE_TIME5 = "02:00"
SCHEDULE_TIME6 = "06:00"

BUTTON_UPDATE = "Update data"                           # Nút Update
BUTTON_STOP = "Stop update"                             # Nút Stop

LABEL_AUTO_ID = "label1"
# =========================

def debug_controls_to_file(window_title, output_file="controls_debug.txt"):
    try:
        print(f"[DEBUG] Kết nối tới cửa sổ '{window_title}'...")
        app = Application(backend="win32").connect(title=window_title, timeout=10)
        window = app.window(title=window_title)

        # Redirect stdout để ghi vào file
        import sys
        original_stdout = sys.stdout

        with open(output_file, "w", encoding="utf-8") as f:
            sys.stdout = f
            window.print_control_identifiers()
            sys.stdout = original_stdout

        print(f"[DEBUG] Đã ghi thông tin controls vào file: {os.path.abspath(output_file)}")

    except Exception as e:
        print(f"[ERROR] Lỗi khi ghi thông tin controls: {e}")
def is_app_running():
    """Kiểm tra xem ứng dụng đã chạy chưa"""
    for proc in psutil.process_iter(['pid', 'name', 'exe']):
        try:
            if proc.info['exe'] and APP_PATH.lower() in proc.info['exe'].lower():
                return True
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
    return False

def start_app():
    """Khởi động ứng dụng"""
    if not is_app_running():
        log_info(f"[{datetime.now()}] Starting datafet tool...")
        subprocess.Popen(APP_PATH)

def stop_app():
    """Kết thúc tiến trình ứng dụng"""
    print(f"[{datetime.now()}] Dừng ứng dụng...")
    for proc in psutil.process_iter(['pid', 'name', 'exe']):
        try:
            if proc.info['exe'] and APP_PATH.lower() in proc.info['exe'].lower():
                proc.kill()
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue

def click_button_by_name(button_name):
    """Click vào nút theo tên trong giao diện"""
    try:
        log_info(f"[ACTION] Connect to window '{WINDOW_TITLE}'...")
        app = Application(backend="win32").connect(title=WINDOW_TITLE, timeout=10)
        log_info('connected to Window')
        window = app.window(title=WINDOW_TITLE)

        log_info(f"[ACTION] Find button '{button_name}'...")
        button = window.child_window(title=button_name, control_type="System.Windows.Forms.Button")

        if button.exists():
            log_info(f"[ACTION] Click vào nút '{button_name}'")
            button.click()
        else:
            log_info(f"[ERROR] Không tìm thấy nút '{button_name}'")

    except Exception as e:
        log_info(f"[ERROR] An error occurred while clicking on '{button_name}': {e}")

def log_info(message):
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{now}] [INFO] {message}")

def get_label_text(window_title, auto_id):
    try:
        app = Application(backend="win32").connect(title=window_title)
        window = app.window(title=window_title)

        label = window.child_window(auto_id=auto_id,
                                    control_type="System.Windows.Forms.Label")

        if label.exists():
            return label.window_text()
        else:
            return None
    except Exception as e:
        print(f"[ERROR] Lỗi khi đọc label: {e}")
        return None    	
def job_check_status():
    log_info(f"[INFO] Kiểm tra label...")
    text = get_label_text(WINDOW_TITLE, LABEL_AUTO_ID)
    if text:
        print(f"Nội dung label: '{text}'")
        if "Done" in text:
            job_restart()
    else:
        print(f"Không đọc được nội dung label.")
def check_status_periodically():
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Đang kiểm tra label...")
    # Gọi hàm monitor_label() hoặc get_label_text() ở đây
    job_check_status()
    # Gọi lại sau 60 giây
    threading.Timer(60, check_status_periodically).start()
# Các job theo lịch trình
def job_start():
    time.sleep(10)  # Chờ 5 giây để ứng dụng load xong
    log_info("job_start")
    #debug_controls_to_file(WINDOW_TITLE)
    click_button_by_name(BUTTON_STOP)
    time.sleep(10)
    click_button_by_name(BUTTON_UPDATE)

def job_stop():
    print(f"[LỊCH TRÌNH] Kết thúc lúc {STOP_TIME}")
    time.sleep(5)  # Chờ 5 giây rồi mới dừng
    click_button_by_name(BUTTON_STOP)
    time.sleep(5)  # Thêm 2s chờ xử lý trước khi kill process
    #stop_app()
def job_stop():
    log_info(f"=====found Done message => restart Datafet")
    click_button_by_name(BUTTON_STOP)
    time.sleep(7)  # Thêm 2s chờ xử lý trước khi kill process
    stop_app()
    log_info("Datafet stopped. Start again")

    start_app()
    job_start()

def save_window_titles_to_file(filename="window_titles.txt"):
    try:
        print(f"[INFO] Đang lấy danh sách tất cả cửa sổ...")
        windows = Desktop(backend="win32").windows()
        
        with open(filename, "w", encoding="utf-8") as f:
            for w in windows:
                title = w.window_text()
                if title:  # Bỏ qua cửa sổ không có tiêu đề
                    f.write(title + "\n")
        
        print(f"[INFO] Đã lưu danh sách tiêu đề cửa sổ vào file: {filename}")

    except Exception as e:
        print(f"[ERROR] Có lỗi xảy ra: {e}")
def main_loop():
    #debug_controls_to_file(WINDOW_TITLE)
    #print("=========Auto datafet=======")
    #return
    #save_window_titles_to_file()
    #print("===========================")
    #return
    start_app()
    job_start()
    schedule.every().day.at(SCHEDULE_TIME1).do(job_start)
    schedule.every().day.at(SCHEDULE_TIME2).do(job_start)
    schedule.every().day.at(SCHEDULE_TIME3).do(job_start)
    schedule.every().day.at(SCHEDULE_TIME4).do(job_start)
    schedule.every().day.at(SCHEDULE_TIME5).do(job_start)
    schedule.every().day.at(SCHEDULE_TIME6).do(job_start)
    check_status_periodically()
	
    print("Starting the schedule...")
    while True:
        schedule.run_pending()
        time.sleep(1)

if __name__ == "__main__":
    main_loop()