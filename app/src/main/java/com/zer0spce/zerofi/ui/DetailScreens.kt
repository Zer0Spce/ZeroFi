package com.zer0spce.zerofi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zer0spce.zerofi.BuildConfig
import com.zer0spce.zerofi.platform.SystemSnapshot
import kotlin.math.roundToInt

@Composable
fun HotspotScreen(active:Boolean?, detectedBand:String?, ssid:String, password:String, security:String, preferredBand:String, onBack:()->Unit, onOpenSystemHotspot:()->Unit, onSaveProfile:(String,String,String,String)->Unit) {
    var localSsid by remember(ssid){ mutableStateOf(ssid) }
    var localPassword by remember(password){ mutableStateOf(password) }
    var localSecurity by remember(security){ mutableStateOf(security) }
    var localBand by remember(preferredBand){ mutableStateOf(preferredBand) }
    DetailPage("Wi-Fi", "Connect to this pocket Wi-Fi", onBack) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val portrait = maxHeight > maxWidth
            if (portrait) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment=Alignment.CenterHorizontally) {
                    WifiConnectionHero(active, detectedBand, localSsid, localPassword, localSecurity)
                    Spacer(Modifier.height(12.dp))
                    WifiSettingsCard(localSsid,localPassword,localSecurity,localBand,{localSsid=it.take(32)},{localPassword=it.take(63)},{localSecurity=it},{localBand=it},onOpenSystemHotspot){onSaveProfile(localSsid,localPassword,localSecurity,localBand)}
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    SurfaceCard(Modifier.weight(.9f).fillMaxHeight()) { WifiConnectionHero(active, detectedBand, localSsid, localPassword, localSecurity, false) }
                    SurfaceCard(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState())) { WifiSettingsContent(localSsid,localPassword,localSecurity,localBand,{localSsid=it.take(32)},{localPassword=it.take(63)},{localSecurity=it},{localBand=it},onOpenSystemHotspot){onSaveProfile(localSsid,localPassword,localSecurity,localBand)} }
                }
            }
        }
    }
}

@Composable private fun WifiConnectionHero(active:Boolean?, detectedBand:String?, ssid:String, password:String, security:String, card:Boolean=true) {
    val content:@Composable ColumnScope.()->Unit = {
        SectionLabel("WI-FI CONNECTION")
        Spacer(Modifier.height(7.dp))
        Text(ssid.ifBlank { "Set Wi-Fi name below" }, color=MaterialTheme.colorScheme.onSurface, fontSize=22.sp, fontWeight=FontWeight.ExtraBold, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth(), maxLines=1, overflow=TextOverflow.Ellipsis)
        Text(when(active){true->"Hotspot is ON";false->"Hotspot is OFF";null->"Hotspot status is system managed"}, color=if(active==true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize=10.sp, fontWeight=FontWeight.SemiBold, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        QrConnectionCard(ssid,password,security, if(card) 232 else 210)
        Spacer(Modifier.height(10.dp))
        Text("Scan this QR code from another device to join the Wi-Fi network shown above.", color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=9.sp, lineHeight=13.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        FeatureLine("Band",detectedBand ?: "System")
        FeatureLine("Security",if(password.isBlank() || security.equals("nopass",true)) "Open" else "WPA/WPA2/WPA3")
    }
    if(card) SurfaceCard(Modifier.fillMaxWidth(),content) else Column(Modifier.fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally, content=content)
}

@Composable private fun WifiSettingsCard(ssid:String,password:String,security:String,band:String,onSsid:(String)->Unit,onPassword:(String)->Unit,onSecurity:(String)->Unit,onBand:(String)->Unit,onOpenSystemHotspot:()->Unit,onSave:()->Unit){
    SurfaceCard(Modifier.fillMaxWidth()){ WifiSettingsContent(ssid,password,security,band,onSsid,onPassword,onSecurity,onBand,onOpenSystemHotspot,onSave) }
}

@Composable private fun WifiSettingsContent(ssid:String,password:String,security:String,band:String,onSsid:(String)->Unit,onPassword:(String)->Unit,onSecurity:(String)->Unit,onBand:(String)->Unit,onOpenSystemHotspot:()->Unit,onSave:()->Unit){
    var showPassword by remember { mutableStateOf(false) }
    SectionLabel("CONNECTION CARD SETTINGS"); Spacer(Modifier.height(8.dp))
    OutlinedTextField(ssid,onSsid,label={Text("Wi-Fi name (SSID)")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Spacer(Modifier.height(7.dp))
    OutlinedTextField(password,onPassword,label={Text("Wi-Fi password")},singleLine=true,visualTransformation=if(showPassword) VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={IconButton(onClick={showPassword=!showPassword}){Icon(if(showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,if(showPassword) "Hide password" else "Show password")}},modifier=Modifier.fillMaxWidth())
    Spacer(Modifier.height(9.dp)); SmallLabel("QR security"); ChoiceRow(listOf("WPA","nopass"),security,onSecurity)
    Spacer(Modifier.height(8.dp)); SmallLabel("Preferred band"); ChoiceRow(listOf("System","2.4 GHz","5 GHz"),band,onBand)
    Spacer(Modifier.height(12.dp)); InfoText("Important: the QR code must match the hotspot name and password configured in Android. Saving here updates ZeroFi's connection card; Android may restrict apps from changing the real hotspot credentials.")
    Spacer(Modifier.height(12.dp)); ActionButton("Save & Refresh QR",Icons.Rounded.Save,onSave)
    Spacer(Modifier.height(8.dp)); ActionButton("Open Android Hotspot Settings",Icons.Rounded.OpenInNew,onOpenSystemHotspot)
}

@Composable private fun QrConnectionCard(ssid:String,password:String,security:String,size:Int){
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
        val valid = ssid.isNotBlank() && (password.isBlank() || security.equals("nopass",true) || password.length>=8)
        val bitmap=remember(ssid,password,security,size){ if(valid) generateQrBitmap(wifiQrPayload(ssid,password,security),size) else null }
        if(bitmap!=null) Image(bitmap.asImageBitmap(),"Wi-Fi QR code",Modifier.size(size.dp).background(Color.White,RoundedCornerShape(18.dp)).padding(10.dp))
        else Box(Modifier.size(size.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(18.dp)),contentAlignment=Alignment.Center){Text(if(ssid.isBlank()) "Enter the hotspot Wi-Fi name" else "Password must be at least 8 characters for secured Wi-Fi",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp,textAlign=TextAlign.Center,modifier=Modifier.padding(18.dp))}
        Spacer(Modifier.height(7.dp)); Text(if(valid) "SCAN TO CONNECT" else "QR NOT READY",fontSize=10.sp,fontWeight=FontWeight.Bold,color=if(valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun DevicesScreen(hotspotActive:Boolean?,onBack:()->Unit,onOpenHotspotSettings:()->Unit){ DetailPage("Connected devices","Hotspot client visibility",onBack){ ResponsiveCards(first={SectionLabel("CLIENT STATUS");Spacer(Modifier.height(12.dp));BigStatus(when(hotspotActive){true->"Hotspot active";false->"Hotspot inactive";null->"Client list restricted"},"ZeroFi only displays client data Android actually exposes.",hotspotActive==true);Spacer(Modifier.height(12.dp));InfoText("Many Android manufacturers keep the hotspot client table behind system permissions. The system hotspot page remains the fallback source of truth.");Spacer(Modifier.height(14.dp));ActionButton("Open Hotspot Device List",Icons.Rounded.Devices,onOpenHotspotSettings)},second={SectionLabel("CLIENT CAPABILITIES");Spacer(Modifier.height(12.dp));FeatureLine("Device count","OEM dependent");FeatureLine("Names / IP / MAC","OEM dependent");FeatureLine("Block / kick","Only when supported");FeatureLine("Android 16+","Richer tethering visibility");Spacer(Modifier.height(12.dp));InfoText("ZeroFi will never fabricate a client count or show a block button that cannot work.")}) } }

@Composable fun DataScreen(snapshot:SystemSnapshot,limitGb:Float,cycleDay:Int,onBack:()->Unit,onOpenUsageAccess:()->Unit,onOpenSystemDataUsage:()->Unit,onLimitChanged:(Float)->Unit,onCycleDayChanged:(Int)->Unit){ var localLimit by remember(limitGb){mutableFloatStateOf(limitGb.coerceIn(1f,500f))};var localDay by remember(cycleDay){mutableFloatStateOf(cycleDay.toFloat())};DetailPage("Mobile data","Usage, allowance & billing cycle",onBack){ResponsiveCards(first={SectionLabel("CURRENT USAGE");Spacer(Modifier.height(12.dp));FeatureLine("This cycle",snapshot.monthBytes?.let(::formatBytes)?:"Unavailable");FeatureLine("Last 3 days",snapshot.threeDayBytes?.let(::formatBytes)?:"Unavailable");FeatureLine("Allowance","${limitGb.roundToInt()} GB");FeatureLine("Cycle reset","Day $cycleDay");Spacer(Modifier.height(12.dp));if(!snapshot.usageAccessGranted){InfoText("Usage Access is required for device-wide mobile-data counters.");Spacer(Modifier.height(8.dp));ActionButton("Grant Usage Access",Icons.Rounded.DataUsage,onOpenUsageAccess);Spacer(Modifier.height(8.dp))};ActionButton("Open Android Data Usage",Icons.Rounded.OpenInNew,onOpenSystemDataUsage)},second={SectionLabel("ZEROFI DATA LIMIT");Spacer(Modifier.height(12.dp));Text("Monthly allowance: ${localLimit.roundToInt()} GB",color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Bold);Slider(localLimit,{localLimit=it;onLimitChanged(it)},valueRange=1f..500f);Spacer(Modifier.height(8.dp));Text("Reset day: ${localDay.roundToInt()}",color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Bold);Slider(localDay,{localDay=it;onCycleDayChanged(it.roundToInt().coerceIn(1,28))},valueRange=1f..28f,steps=26);InfoText("ZeroFi tracks usage for display and warnings; it does not change carrier billing.")})} }

@Composable fun DisplayScreen(keepScreenOn:Boolean,immersiveMode:Boolean,oledSaver:Boolean,onBack:()->Unit,onKeepScreenOnChanged:(Boolean)->Unit,onImmersiveModeChanged:(Boolean)->Unit,onOledSaverChanged:(Boolean)->Unit,onBrightnessChanged:(Float)->Unit,onSystemBrightness:()->Unit){var brightness by remember{mutableFloatStateOf(.65f)};DetailPage("Display","Screen & power behavior",onBack){ResponsiveCards(first={SectionLabel("SCREEN BEHAVIOR");Spacer(Modifier.height(8.dp));SettingSwitch("Keep screen awake","Optional. Off lets the phone sleep normally.",keepScreenOn,onCheckedChange=onKeepScreenOnChanged);SettingSwitch("Immersive fullscreen","Hide status and navigation bars",immersiveMode,onCheckedChange=onImmersiveModeChanged)},second={SectionLabel("BRIGHTNESS");Spacer(Modifier.height(12.dp));Text("ZeroFi brightness: ${(brightness*100).roundToInt()}%",color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Bold);Slider(brightness,{brightness=it;onBrightnessChanged(it)},valueRange=.05f..1f);Spacer(Modifier.height(8.dp));InfoText("The screen may sleep while hotspot and Router Lock continue. Android/OEM power management still controls background tethering behavior.");Spacer(Modifier.height(12.dp));ActionButton("Use System Brightness",Icons.Rounded.Brightness6,onSystemBrightness)})} }

@Composable fun SettingsScreen(settings:ZeroFiSettingsState,deviceOwner:Boolean,lockRunning:Boolean,dedicatedModeSupported:Boolean,onBack:()->Unit,onAutoLaunchChanged:(Boolean)->Unit,onRouterLockToggle:()->Unit,onDedicatedBatterySaverChanged:(Boolean)->Unit,onOpenAppSettings:()->Unit,onDiagnostics:()->Unit,onStandby:()->Unit){DetailPage("Router settings","ZeroFi appliance controls",onBack){ResponsiveCards(first={SectionLabel("ROUTER MODE");Spacer(Modifier.height(8.dp));SettingSwitch("Launch after boot","Open ZeroFi after Android starts",settings.autoLaunchOnBoot,onCheckedChange=onAutoLaunchChanged);SettingSwitch("Dedicated Battery Saver",if(dedicatedModeSupported)"Suspend ordinary user apps while ZeroFi runs" else "Requires Android 7+ and Device Owner",settings.dedicatedBatterySaver,dedicatedModeSupported,onDedicatedBatterySaverChanged);Spacer(Modifier.height(8.dp));ActionButton(if(lockRunning)"Exit Router Lock" else "Enable Router Lock",Icons.Rounded.Lock,onRouterLockToggle);Spacer(Modifier.height(8.dp));ActionButton("Standby display",Icons.Rounded.Bedtime,onStandby);Spacer(Modifier.height(8.dp));ActionButton("Router diagnostics",Icons.Rounded.Info,onDiagnostics);Spacer(Modifier.height(12.dp));FeatureLine("Device Owner",if(deviceOwner)"Configured" else "Not configured");FeatureLine("Router Lock",if(lockRunning)"Active" else "Inactive");FeatureLine("Theme",if(settings.darkMode)"Dark" else "Light")},second={SectionLabel("DEVICE OWNER SETUP");Spacer(Modifier.height(9.dp));InfoText("Full kiosk mode and user-app suspension require ZeroFi to be Device Owner on a dedicated phone.");Spacer(Modifier.height(10.dp));Text("adb shell dpm set-device-owner\ncom.zer0spce.zerofi/.admin.ZeroFiDeviceAdminReceiver",color=MaterialTheme.colorScheme.primary,fontFamily=FontFamily.Monospace,fontSize=10.sp,modifier=Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f),RoundedCornerShape(12.dp)).padding(10.dp));Spacer(Modifier.height(12.dp));ActionButton("Open ZeroFi App Settings",Icons.Rounded.Settings,onOpenAppSettings);Spacer(Modifier.height(8.dp));Text("ZeroFi ${BuildConfig.VERSION_NAME}",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp)})} }

@Composable private fun DetailPage(title:String,subtitle:String,onBack:()->Unit,content:@Composable ()->Unit){Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal=14.dp,vertical=12.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButtonTile(Icons.Rounded.ArrowBack,"Back",onBack);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(title,color=MaterialTheme.colorScheme.onBackground,fontSize=22.sp,fontWeight=FontWeight.ExtraBold);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}};Spacer(Modifier.height(12.dp));Box(Modifier.weight(1f).fillMaxWidth()){content()}}}
@Composable private fun ResponsiveCards(firstWeight:Float=1f,secondWeight:Float=1f,first:@Composable ColumnScope.()->Unit,second:@Composable ColumnScope.()->Unit){BoxWithConstraints(Modifier.fillMaxSize()){if(maxHeight>maxWidth)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){SurfaceCard(modifier=Modifier.fillMaxWidth(),content=first);SurfaceCard(modifier=Modifier.fillMaxWidth(),content=second);Spacer(Modifier.height(4.dp))}else Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){SurfaceCard(modifier=Modifier.weight(firstWeight).fillMaxHeight(),content=first);SurfaceCard(modifier=Modifier.weight(secondWeight).fillMaxHeight(),content=second)}}}
@Composable private fun SurfaceCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){val shape=RoundedCornerShape(20.dp);Column(modifier.shadow(3.dp,shape).background(MaterialTheme.colorScheme.surface,shape).border(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.20f),shape).padding(14.dp),content=content)}
@Composable private fun SectionLabel(text:String){Text(text,color=MaterialTheme.colorScheme.primary,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp)}
@Composable private fun SmallLabel(text:String){Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp)}
@Composable private fun BigStatus(title:String,subtitle:String,positive:Boolean){Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),RoundedCornerShape(14.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(10.dp).background(if(positive)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,CircleShape));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(title,color=MaterialTheme.colorScheme.onSurface,fontSize=15.sp,fontWeight=FontWeight.Bold);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp)}}}
@Composable private fun InfoText(text:String){Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){Icon(Icons.Rounded.Info,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(15.dp));Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp,lineHeight=14.sp)}}
@Composable private fun FeatureLine(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp,modifier=Modifier.weight(1f));Text(value,color=MaterialTheme.colorScheme.onSurface,fontSize=10.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)}}
@Composable private fun SettingSwitch(title:String,subtitle:String,checked:Boolean,enabled:Boolean=true,onCheckedChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=if(enabled)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp,lineHeight=12.sp)};Spacer(Modifier.width(10.dp));Switch(checked,onCheckedChange,enabled=enabled)}}
@Composable private fun ChoiceRow(values:List<String>,selected:String,onSelect:(String)->Unit){Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){values.forEach{value->val chosen=value==selected;Text(value,color=if(chosen)Color.Black else MaterialTheme.colorScheme.onSurface,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.background(if(chosen)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(9.dp)).clickable{onSelect(value)}.padding(horizontal=9.dp,vertical=6.dp))}}}
@Composable private fun ActionButton(text:String,icon:ImageVector,onClick:()->Unit){val shape=RoundedCornerShape(13.dp);Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha=.09f),shape).border(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=.28f),shape).clickable(onClick=onClick).padding(horizontal=12.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text(text,color=MaterialTheme.colorScheme.onSurface,fontSize=10.sp,fontWeight=FontWeight.SemiBold)}}
@Composable private fun IconButtonTile(icon:ImageVector,label:String,onClick:()->Unit){Row(Modifier.background(MaterialTheme.colorScheme.surface,RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,label,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text(label,color=MaterialTheme.colorScheme.onSurface,fontSize=10.sp,fontWeight=FontWeight.SemiBold)}}
