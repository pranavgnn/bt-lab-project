$ErrorActionPreference = "Stop"
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$wpf = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF"

& $csc /nologo /target:winexe /optimize+ /out:Capri.exe /win32icon:assets\chip.ico `
    /r:System.dll `
    /r:System.Core.dll `
    /r:System.Drawing.dll `
    /r:System.IO.Compression.FileSystem.dll `
    /r:System.Net.Http.dll `
    /r:System.Runtime.Serialization.dll `
    /r:System.Security.dll `
    /r:"$wpf\System.Speech.dll" `
    /r:System.Windows.Forms.dll `
    /r:System.Xml.dll `
    /r:"$wpf\UIAutomationClient.dll" `
    /r:"$wpf\UIAutomationTypes.dll" `
    /r:"$wpf\WindowsBase.dll" `
    /resource:assets\chip.ico,Capri.Chip.ico `
    /resource:assets\fonts\ShareTechMono-Regular.ttf,Capri.Font.Mono.ttf `
    /resource:assets\sprites\idle.png,Capri.Sprite.idle.png `
    /resource:assets\sprites\blink.png,Capri.Sprite.blink.png `
    /resource:assets\sprites\blink_glitch.png,Capri.Sprite.blink_glitch.png `
    /resource:assets\sprites\grin.png,Capri.Sprite.grin.png `
    /resource:assets\sprites\glitch.png,Capri.Sprite.glitch.png `
    /resource:assets\sprites\dangle.png,Capri.Sprite.dangle.png `
    /resource:assets\sprites\sleep.png,Capri.Sprite.sleep.png `
    /resource:assets\sprites\wake.png,Capri.Sprite.wake.png `
    /resource:assets\sprites\perch.png,Capri.Sprite.perch.png `
    /resource:assets\sprites\perch_blink.png,Capri.Sprite.perch_blink.png `
    /resource:assets\sprites\perch_blink_glitch.png,Capri.Sprite.perch_blink_glitch.png `
    /resource:assets\sprites\perch_glitch.png,Capri.Sprite.perch_glitch.png `
    /resource:assets\sprites\mouth.png,Capri.Sprite.mouth.png `
    /resource:assets\sprites\box_talk.png,Capri.Sprite.box_talk.png `
    /resource:assets\sprites\box_listen.png,Capri.Sprite.box_listen.png `
    /resource:assets\sprites\box_eyes.png,Capri.Sprite.box_eyes.png `
    /resource:assets\sprites\holo_banner.png,Capri.Sprite.holo_banner.png `
    /resource:assets\sprites\holo_call.png,Capri.Sprite.holo_call.png `
    /resource:assets\sprites\hi_box.png,Capri.Sprite.hi_box.png `
    /resource:assets\sprites\box_tenor_talk.png,Capri.Sprite.box_tenor_talk.png `
    /resource:assets\sprites\box_tenor_glitch.png,Capri.Sprite.box_tenor_glitch.png `
    /resource:assets\sprites\box_canon_smirk.png,Capri.Sprite.box_canon_smirk.png `
    /resource:assets\sprites\box_canon_intense.png,Capri.Sprite.box_canon_intense.png `
    /resource:assets\sprites\box_canon_neon.png,Capri.Sprite.box_canon_neon.png `
    /resource:assets\sprites\box_canon_holo.png,Capri.Sprite.box_canon_holo.png `
    /resource:assets\sprites\screen_takeover.png,Capri.Sprite.screen_takeover.png `
    /resource:assets\sprites\box_cutins.png,Capri.Sprite.box_cutins.png `
    app\Agent.cs app\AssemblyInfo.cs app\Brain.cs app\Capri.cs app\Ears.cs app\Engine.cs app\Ledges.cs app\Mainframe.cs app\Talk.cs app\Voice.cs

if ($LASTEXITCODE -ne 0) {
    Write-Error "CSC failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Successfully compiled Capri.exe" -ForegroundColor Cyan
