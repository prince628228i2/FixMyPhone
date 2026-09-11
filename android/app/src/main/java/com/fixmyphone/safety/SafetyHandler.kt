package com.fixmyphone.safety
import com.fixmyphone.executor.*
class SafetyHandler:ActionHandler{override fun canHandle(a:ActionType)=true;override fun execute(a:FixMyPhoneAction):String=if(ActionGuardRails.allow(a))"requires_user" else "failed"}
