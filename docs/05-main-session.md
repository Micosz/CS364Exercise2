# 05-main-session

เริ่มรอบ สังเกตเวลาที่เพิ่มขึ้น แล้วจบและบันทึก ทดสอบการพักเวลาเมื่อสลับไปแอปอื่นและกลับมา เมื่อรวมเซนเซอร์แล้วใช้รอบ 60 ก้าวใน 2 นาทีตรวจค่าเฉลี่ย 30 ก้าว/นาที ตรวจยอดรวมหลังบันทึกสองรอบ สถิติแยกก้าวและเขย่า รอบที่สูงกว่าเดิมอย่างน้อยหนึ่งด้านจะแสดง New Record คะแนนเสมอจะไม่ขึ้นสถิติใหม่ เมื่อลบรอบสูงสุด สถิติจะคำนวณใหม่

เปิดโฟลเดอร์ StepByStep ใน Android Studio และ Sync ก่อน Run โมดูล app เมื่อทดสอบแล้วใช้ `git status -sb` ตรวจว่าอยู่บน `feature/main-session` แล้ว `git push -u origin HEAD` เพื่อเปิด PR เข้า main

## บันทึกวิธีทดสอบและผลที่พบ

- **Build**: ทดสอบด้วย `./gradlew assembleDebug` (JDK 21, compileSdk 37, minSdk 26) ผ่านสมบูรณ์ (`BUILD SUCCESSFUL`)
- **ตรวจสอบสูตรค่าเฉลี่ย (`SessionStats.perMinute`)**:
  - `SessionStats.perMinute(60, 120000L)` = `30.0` ก้าว/นาที, `SessionStats.perMinute(10, 120000L)` = `5.0` ครั้ง/นาที (รอบ A)
  - `SessionStats.perMinute(90, 60000L)` = `90.0` ก้าว/นาที, `SessionStats.perMinute(6, 60000L)` = `6.0` ครั้ง/นาที (รอบ B)
  - กรณีเวลาเป็น 0: `SessionStats.perMinute(10, 0L)` = `0.0` (ไม่เกิด `Infinity` หรือ `NaN`)
- **ตรวจสอบยอดรวม สถิติสูงสุด และป้าย `New Record` (`SessionStats.from`)**:
  - ทดสอบรอบ A `(1, 1000L, 120000L, 60, 10)` และรอบ B `(2, 2000L, 60000L, 90, 6)` โดยส่ง `[B, A]`:
    - ได้ `totalSteps = 150`, `totalShakes = 16`, `bestSteps = 90`, `bestShakes = 10`, `newStepRecord = true`, `newShakeRecord = false` แสดง `New Record · ก้าวเดิน`
  - กรณีคะแนนเสมอ: เมื่อเพิ่มรอบ C `(3, 3000L, 60000L, 90, 10)` เทียบกับค่าสูงสุดเดิม ได้ `newStepRecord = false`, `newShakeRecord = false` (ไม่แสดงป้ายสถิติใหม่)
  - กรณีลบรอบสูงสุด (ลบรอบ B เหลือรอบ A): คำนวณใหม่ได้ `totalSteps = 60`, `totalShakes = 10`, `bestSteps = 60`, `bestShakes = 10`
  - กรณีลบทั้งหมด (รายการว่าง `[]`): ได้ยอดรวมและสถิติเป็น `0` ทั้งหมด และซ่อนป้าย `New Record` (`View.GONE`)
- **ตรวจสอบการจับเวลาและสถานะหน้าหลัก (`MainActivity` + `SessionPanel`)**:
  - จับเวลาด้วย `SystemClock.elapsedRealtime()` พักเวลาอัตโนมัติใน `onPause()` และเดินเวลาต่อใน `onResume()` รวมถึงคงสถานะผ่าน `onSaveInstanceState()` เมื่อหมุนหน้าจอ

