package com.fixmyphone.safety
object EmergencyStopManager { @Volatile var stopped=false; fun stop(){stopped=true}; fun reset(){stopped=false} }
