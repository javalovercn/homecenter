用cron来驱动脚本是件非常轻松的事。

以下示例是实现每30秒执行一次脚本，星期天除外。

代码如下：
```jruby
import Java::hc.server.util.calendar.WeeklyJobCalendar
import java.lang.StringBuilder

ctx = Java::hc.server.ui.ProjectContext::getProjectContext()
scheduler = ctx.getScheduler("MyScheduler1")
scheduler.start()
if scheduler.isExistsTrigger("Trigger1") == false
  builder = StringBuilder.new(100)
  builder.append("ctx = Java::hc.server.ui.ProjectContext::getProjectContext()\n")
  builder.append("ctx.showTipOnTray(\"executing job1\")\n")
  scheduler.addJob("Job1", builder.toString())

  weeklyCalendar = WeeklyJobCalendar.new()
  weeklyCalendar.setDayExcluded(java.util.Calendar::SUNDAY, true)#exclude sunday
  weeklyCalendar.setDayExcluded(java.util.Calendar::SATURDAY, false)
  scheduler.addCalendar("Calendar1", weeklyCalendar)

  scheduler.addCronTrigger("Trigger1", "0/30 * * * * ?", "Calendar1", "Job1")#trigger "Job1" when "Calendar1" every 30 seconds and exclude sunday
  puts "Trigger1 next fire time : " + scheduler.getTriggerNextFireTime("Trigger1").toString()
end
```

### 调试总管
```jruby
scheduler = ctx.getScheduler("MyScheduler1")
```
每一个调度总管(scheduler)存入一个数据库，它管理一个或多个相互关联的任务（Job），日历（JobCalendar）和触发器（Trigger）。

一个工程内可以使用多个调度总管。

### 任务
```jruby
scheduler.addJob("Job1", builder.toString())
```
添加或重新定义一个任务，名称为"Job1"，它的执行脚本是第二个参数。

### 工作日历
```jruby
scheduler.addCalendar("Calendar1", weeklyCalendar)
```
添加或重新定义工作日历(JobCalendar)，本示例工作日历是周日不执行外，其它星期都正常执行。

工作日历有以下参考实现（你也可定义自己的JobCalendar）：
1. [AnnualJobCalendar](http://homecenter.mobi/download/javadoc/hc/server/util/calendar/AnnualJobCalendar.html), 可用于排除一年中某些天不执行。
2. [CronExcludeJobCalendar](http://homecenter.mobi/download/javadoc/hc/server/util/calendar/CronExcludeJobCalendar.html)，用于表示cron表达式指定的时间为非执行时间，比如8AM-5PM不能执行的表达式为："* * 0-7,18-23 ? * *"。了解更多cron表达式，请参阅http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html
3. [DailyJobCalendar](http://homecenter.mobi/download/javadoc/hc/server/util/calendar/DailyJobCalendar.html)，用于指定一天中的某个时间段为非执行时间或执行时间，
4. [HolidayJobCalendar](http://homecenter.mobi/download/javadoc/hc/server/util/calendar/HolidayJobCalendar.html)，用于指定某些年份的日期为非执行时间，比如未来两年的某天为非执行日，只需添加两个日期条目即可，
5. [MonthlyJobCalendar](http://homecenter.mobi/download/javadoc/hc/server/util/calendar/MonthlyJobCalendar.html)，用于指定每个月份中的某些天为非执行日，
6. [WeeklyJobCalendar](http://homecenter.mobi/download/javadoc/hc/server/util/calendar/WeeklyJobCalendar.html)，用于指定每周中的某几天为非执行日，

以上工作日历可组合使用。

### 触发器
```jruby
scheduler.addCronTrigger("Trigger1", "0/30 * * * * ?", "Calendar1", "Job1")
```
添加并开始调度触发器,该触发条件是满足一个cron表达式和工作日历"Calendar1"，即非周日每30秒执行一次，执行任务是"Job1"。

任务"Job1"可被多个触发器使用。
