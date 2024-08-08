;+
; CLASS_NAME:
;  TimeUtil
; PURPOSE:
;  This gets closer to a real class which might do something.  These are
;  static methods, used to operate on a prescribed representation of time.
; METHODS:
;  TimeUtil::create
;  TimeUtil::print
;  TimeUtil::addComponent
;-

;+
; Purpose:
;   create a time array
;
; Parameters:
;   h - the hours
;   m - the minutes
;   s - the seconds
;-
function TimeUtil::create, h, m, s
    compile_opt idl2, static
    return, [ h, m, s]
end

;+
; TimeUtil::print
;
; Purpose:
;   print the time to stdout.
; Parameters:
;   time - three-component time array
; Keywords:
;   none
;-
pro TimeUtil::print, time
    compile_opt idl2, static
    ;TimeUtil= obj_new('TimeUtil')
    print,TimeUtil.toString(time)
end

;+
; Add a component, hours or minutes or seconds, to the time.
;
; Parameters:
;   time - a three-element int array of [ hours, minutes, seconds ]
;   component - SECOND or MINUTE or HOUR
;   s - the number of that component
;-
pro TimeUtil::addComponent, time, component, s
    compile_opt idl2, static
    common TimeUtil
    if component eq TimeUtil_SECOND then begin
        time[TimeUtil_SECOND] = time[TimeUtil_SECOND] + s
    endif else if component eq TimeUtil_MINUTE then begin
        time[TimeUtil_MINUTE] = time[TimeUtil_MINUTE] + s
    endif else if component eq TimeUtil_HOUR then begin
        time[TimeUtil_HOUR] = time[TimeUtil_HOUR] + s
    endif
    while time[TimeUtil_SECOND] gt 60 do begin
        time[TimeUtil_MINUTE] = time[TimeUtil_MINUTE] + 1
        time[TimeUtil_SECOND] = time[TimeUtil_SECOND] - 60
    endwhile
    if time[TimeUtil_MINUTE] gt 60 then begin
        hr = time[TimeUtil_MINUTE] / 60
        mn = time[TimeUtil_MINUTE] - 60 * hr
        time[TimeUtil_HOUR] = time[TimeUtil_HOUR] + hr
        time[TimeUtil_MINUTE] = time[TimeUtil_MINUTE] - hr * 60 + mn
    endif 
end

function TimeUtil::toString, time
    compile_opt idl2, static
    return, string(format='%02d:%02d:%02d',time[0], time[1], time[2])
end

function TimeUtil::Init
    print, 'TimeUtil::Init'
    compile_opt idl2, static
    common TimeUtil, TimeUtil
    return, 1
end


pro TimeUtil__define
    print, 'TimeUtil__define'
    common TimeUtil, TimeUtil_HOUR, TimeUtil_MINUTE, TimeUtil_SECOND
    TimeUtil = { TimeUtil, dummy:0 }
    TimeUtil_HOUR=0
    TimeUtil_MINUTE=1
    TimeUtil_SECOND=2
    return
end
