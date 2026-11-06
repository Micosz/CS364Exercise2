# StepByStep

แอป Android สำหรับ CS364 Assignment 2 นับก้าวและการเขย่าแยกตามรอบกิจกรรม

เปิดโฟลเดอร์ที่มี `settings.gradle` ใน Android Studio จากนั้น Sync และ Run โมดูล `app` ใช้ Android SDK 37 และ JDK 17 ขึ้นไป (แนะนำ JDK 21 ที่มากับ Android Studio)

`main` เริ่มจากหน้าจอและข้อตกลงข้อมูลร่วมกัน สมาชิกเปิด PR จาก branch ที่อยู่ใน ZIP ของตน เมื่อรวมครบห้าส่วนจึงได้ทั้งการนับ สถิติ ฐานข้อมูล และประวัติ

หลังแตก ZIP ให้เปิดโฟลเดอร์ StepByStep ตรวจ branch ด้วย `git status -sb` ทดสอบงาน ปรับตามที่ต้องการ แล้วใช้ `git push -u origin HEAD` เพื่อเปิด PR ไปยัง `main` หาก push ตรงไม่ได้ ให้ขอสิทธิ์ collaborator หรือ push branch ไปยัง fork ของตัวเอง

เมื่อส่งงาน ให้ระบุชื่อและหน้าที่จริงของสมาชิกทั้งห้าคน พร้อมแนบผลทดลองตาม Checklist
