var setupTask;
var nextTime;

function init() {
    if (em.getChannel() == 5) {
        em.setWorldEvent();
        em.scheduleRandomEvent();
        return;
        var cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR, 1);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        nextTime = cal.getTimeInMillis();
        while (nextTime <= java.lang.System.currentTimeMillis()) {
            nextTime += 1000 * 60 * 60;
        }
        scheduleNew();
    }
}

function scheduleNew() {
    setupTask = em.scheduleAtTimestamp("setup", nextTime);
    em.setWorldEvent();
}

function cancelSchedule() {
    if (setupTask != null) {
        setupTask.cancel(true);
    }
}

function setup() {
    em.scheduleRandomEvent();
    setupTask = em.schedule("scheduleNew", 120000);
    nextTime += 1000 * 60 * 60;
}