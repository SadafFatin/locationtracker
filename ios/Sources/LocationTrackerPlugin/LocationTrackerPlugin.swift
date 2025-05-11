import Foundation
import CoreLocation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(LocationTrackerPlugin)
public class LocationTrackerPlugin: CAPPlugin, CAPBridgedPlugin, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    public let identifier = "LocationTrackerPlugin"
    public let jsName = "LocationTrackerPlugin";
    private let implementation = LocationTracker()
    public enum NotificationType{
        case  START_TRACKING,STOP_TRACKING,UPDATE_TRACKING
    }
    public final let NOTIFICATION_TRACKING_IDENTIFIER = "TRACKING_NOTIFICATION";
    public final let NOTIFICATION_STOP_TRACKING_IDENTIFIER = "TRACKING_NOTIFICATION";

    public var baseUrl = "";
    private var watchers = [Watcher]();
    
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "trackLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopTrackingLocation", returnType: CAPPluginReturnPromise)
    ]
    
    @objc public override func load() {
        UIDevice.current.isBatteryMonitoringEnabled = true;
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, error in
               if granted {
                   print("Notifications allowed")
               }
        }
      
    }
    
    
    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.echo("this was sent to plugin"+value)
        ])
    }
    
    @objc func trackLocation(_ call: CAPPluginCall) {
        call.keepAlive = true
        baseUrl = call.getString("baseUrl") ?? ""
        //making backgroud tracking always true
        if(call.getString("backgroundMessage") != nil){
            call.setValue("Tracking Loaction", forKey: "backgroundMessage");
        }
        
        // CLLocationManager requires main thread
        DispatchQueue.main.async {
            let background = call.getString("backgroundMessage") != nil
            
            let watcher = Watcher(
                call.callbackId,
                stale: call.getBool("stale") ?? false
            )
            let manager = watcher.locationManager
            manager.delegate = self
            let externalPower = [
                .full,
                .charging
            ].contains(UIDevice.current.batteryState)
            manager.desiredAccuracy = (
                externalPower
                ? kCLLocationAccuracyBestForNavigation
                : kCLLocationAccuracyBest
            )
            var distanceFilter = call.getDouble("distanceFilter")
            // It appears that setting manager.distanceFilter to 0 can prevent
            // subsequent location updates. See issue #88.
            if distanceFilter == nil || distanceFilter == 0 {
                distanceFilter = kCLDistanceFilterNone
            }
            manager.distanceFilter = distanceFilter!
            manager.allowsBackgroundLocationUpdates = true
            manager.showsBackgroundLocationIndicator = true
            
            manager.pausesLocationUpdatesAutomatically = false
            self.watchers.append(watcher)
            if call.getBool("requestPermissions") != false {
                let status = CLLocationManager.authorizationStatus()
                if [
                    .notDetermined,
                    .denied,
                    .restricted,
                ].contains(status) {
                    return (
                        background
                        ? manager.requestAlwaysAuthorization()
                        : manager.requestWhenInUseAuthorization()
                    )
                }
                if (
                    background && status == .authorizedWhenInUse
                ) {
                    // Attempt to escalate.
                    manager.requestAlwaysAuthorization()
                }
            }
            return watcher.start()
        }
    }
    
    @objc func stopTrackingLocation(_ call: CAPPluginCall) {
        // CLLocationManager requires main thread
        DispatchQueue.main.async {
            if let callbackId = call.getString("id") {
                if let index = self.watchers.firstIndex(
                    where: { $0.callbackId == callbackId }
                ) {
                    self.watchers[index].locationManager.stopUpdatingLocation()
                    self.watchers.remove(at: index)
                }
                if let savedCall = self.bridge?.savedCall(withID: callbackId) {
                    self.bridge?.releaseCall(savedCall)
                }
                return call.resolve([
                    "value": "Stopping Location Tracking"
                ])
            }
            else{
                self.stopTracking(call:call)
            }
        }
    }
    
    func stopTracking(call :CAPPluginCall?){
        self.watchers.map{ Watcher in
            Watcher.stop();
            return call?.resolve([
                "value": "Stopped Location Tracking"
            ]);
        }
        self.sendNotification(type: NotificationType.STOP_TRACKING, contentText: "Tracking Stopped");
    }

    
    public func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        if let watcher = self.watchers.first(
            where: { $0.locationManager == manager }
        ) {
            if let call = self.bridge?.savedCall(withID: watcher.callbackId) {
                if let clErr = error as? CLError {
                    if clErr.code == .locationUnknown {
                        // This error is sometimes sent by the manager if
                        // it cannot get a fix immediately.
                        return
                    } else if (clErr.code == .denied) {
                        watcher.stop()
                        return call.reject(
                            "Permission denied.",
                            "NOT_AUTHORIZED"
                        )
                    }
                }
                
                return call.reject(error.localizedDescription, nil, error)
            }
        }
    }
    
    
    
    public func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        if let location = locations.last {
            if let watcher = self.watchers.first(
                where: { $0.locationManager == manager }
            ) {
                if watcher.isLocationValid(location) {
                    if let call = self.bridge?.savedCall(withID: watcher.callbackId) {
                        
                        print("location reveived -- lat: ",  location.coordinate.latitude , " long: " , location.coordinate.longitude,
                              " time at: " , Date())
                        postLocationToServer(lat: location.coordinate.latitude, lon: location.coordinate.longitude)
                        sendNotification(type: NotificationType.UPDATE_TRACKING, contentText: "Location Updated")
                        return call.resolve(formatLocation(location))
                    }
                }
            }
        }
    }
    
    public func userNotificationCenter(_ center: UNUserNotificationCenter,
                                    didReceive response: UNNotificationResponse,
                                    withCompletionHandler completionHandler: @escaping () -> Void) {
           if response.actionIdentifier == "STOP_TRACKING" {
               print("Stop button tapped")
           }
           completionHandler()
    }
    
    
    func sendNotification(type:NotificationType, title: String =  "Tracking",contentText :String = "Location Tracking update") {
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = contentText
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
            let request = UNNotificationRequest(identifier: NOTIFICATION_TRACKING_IDENTIFIER, content: content, trigger: trigger)
            let notiCenter = UNUserNotificationCenter.current()

            if(type == NotificationType.UPDATE_TRACKING || type == NotificationType.START_TRACKING) {
                let closeAction = UNNotificationAction(identifier: NOTIFICATION_STOP_TRACKING_IDENTIFIER, title: "Stop Tracking", options: [.foreground]);
                let category = UNNotificationCategory(identifier: NOTIFICATION_TRACKING_IDENTIFIER, actions: [closeAction], intentIdentifiers: [], options: []);
                notiCenter.setNotificationCategories([category])
                notiCenter.delegate = self
            }
            notiCenter.add(request)
        }
    
    func postLocationToServer(lat: Double, lon: Double) {
        print("posting data to server: " , baseUrl)
        guard let url = URL(string: baseUrl) else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let payload = ["latitude": lat, "longitude": lon]
        request.httpBody = try? JSONSerialization.data(withJSONObject: payload, options: [])
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("Post error: \(error)")
            } else {
                print("Location posted: \(lat), \(lon)")
            }
        }.resume()
    }
    
    func formatLocation(_ location: CLLocation) -> PluginCallResultData {
        let null = Optional<Double>.none as Any
        
        var simulated = false;
        if #available(iOS 15, *) {
            // Prior to iOS 15, it was not possible to detect simulated locations.
            // But in general, it is very difficult to simulate locations on iOS in
            // production.
            if location.sourceInformation != nil {
                simulated = location.sourceInformation!.isSimulatedBySoftware;
            }
        }
        return [
            "lat": location.coordinate.latitude,
            "lon": location.coordinate.longitude,
            "accuracy": location.horizontalAccuracy,
            "altitude": location.altitude,
            "altitudeAccuracy": location.verticalAccuracy,
            "simulated": simulated,
            "speed": location.speed < 0 ? null : location.speed,
            "bearing": location.course < 0 ? null : location.course,
            "time": NSNumber(
                value: Int(
                    location.timestamp.timeIntervalSince1970 * 1000
                )
            ),
        ]
    }
    

    class Watcher {
        private let NOTIFICATION_TRACKING_IDENTIFIER = "TRACKING_NOTIFICATION"
        let callbackId: String
        let locationManager: CLLocationManager = CLLocationManager()
        private let created = Date()
        private let allowStale: Bool
        private var isUpdatingLocation: Bool = false
        init(_ id: String, stale: Bool) {
            callbackId = id
            allowStale = stale
        }
        
        func sendLocalNotification() {
                let content = UNMutableNotificationContent()
                content.title = "Tracking Started"
                content.body = "Live location updates have been started."

                let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
                let request = UNNotificationRequest(identifier: NOTIFICATION_TRACKING_IDENTIFIER, content: content, trigger: trigger)
                let notificationCenter = UNUserNotificationCenter.current()
                notificationCenter.add(request, withCompletionHandler: nil)

        }
        
        
        func start() {
            // Avoid unnecessary calls to startUpdatingLocation, which can
            // result in extraneous invocations of didFailWithError.
            if !isUpdatingLocation {
                //locationManager.startUpdatingLocation()
                locationManager.startMonitoringSignificantLocationChanges();
                isUpdatingLocation = true
                sendLocalNotification();
            }
        }
        func stop() {
            if isUpdatingLocation {
                locationManager.stopUpdatingLocation()
                isUpdatingLocation = false
            }
        }
        func isLocationValid(_ location: CLLocation) -> Bool {
            return (
                allowStale ||
                location.timestamp >= created
            )
        }
    }
    
   
}
 
