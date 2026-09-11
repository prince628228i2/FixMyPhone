package com.fixmyphone.safety
import com.fixmyphone.executor.*
object ActionGuardRails {
 fun allow(a:FixMyPhoneAction):Boolean { if(EmergencyStopManager.stopped)return false; if(a.action==ActionType.CALL && !a.requiresConfirmation)return false; return true }
}
