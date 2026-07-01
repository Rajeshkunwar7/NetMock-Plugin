# 📡 NetMock 

A lightweight, developer-centric network manipulation dashboard built directly inside Android Studio. 

NetMock eliminates the need for heavy, complex external proxy software (like Charles Proxy, Wireshark, or Chucker) when testing mobile app resilience. With an intuitive sidebar panel, NetMock establishes a secure ADB tunnel straight into your running application, allowing you to distort, delay, and disrupt network traffic on the fly with zero compilation downtime.

---

## ✨ Features

*   **⚡ Precision Latency Emulation**: Simulate real-world network degradations (Subway gaps, elevator drops, or poor connectivity) by instantly forcing network hold intervals from 1 to 20 seconds.
*   **🔬 Targeted Interception Control**: Toggle between broad **Global interception** or precision surgery using **REST filters** and **GraphQL Operation Name matching**.
*   **🎭 Native GraphQL Support**: Gracefully intercept unified `/graphql` single-endpoint paths. Automatically inject properly structured JSON `{"errors": [...]}` structures while maintaining valid `200 OK` status envelopes.
*   **🔀 Smart Rule Matrix**: Test UI stability under infrastructure failure conditions by forcing explicit server status code overrides (e.g., `401 Unauthorized`, `500 Server Error`, or client socket timeouts).
*   **🪶 Zero-Overhead Mobile Agent**: Powered by a highly optimized, lightweight OkHttp Interceptor that runs isolated inside your debug builds with zero impact on production bundle sizes.

---

## 🛠️ Installation & Setup

Using NetMock is a simple **3-step integration** that takes less than two minutes.

### 1. Install the Android Studio Plugin
1. Open **Android Studio** and navigate to **Settings** (or **Preferences** on macOS) > **Plugins**.
2. Go to the **Marketplace** tab, search for **`NetMock`**, and click **Install**.
3. Restart your IDE. You will see a brand-new **`NetMock Dashboard`** tab appear on your right-hand sidebar menu tray.

### 2. Add the Mobile Agent Dependency
Add the lightweight runtime utility library into your application's **app-level** `build.gradle` (or `build.gradle.kts`) dependencies block:

```groovy
dependencies {
    // We use debugImplementation so the agent code is completely stripped out of release builds!
    debugImplementation 'com.github.yourusername:NetMockAgent:1.0.0'
}
```

### 3. Attach the Interceptor to OkHttp
Drop the `NetMockInterceptor` directly into your network engine initialization chain (such as inside a Koin, Dagger Hilt, or manual OkHttp configuration block):

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .apply {
        // Only attach NetMock in debug environments to secure production traffic
        if (BuildConfig.DEBUG) {
            addInterceptor(com.agent.networkinjector.interceptor.NetMockInterceptor())
        }
    }
    .connectTimeout(5, TimeUnit.MINUTES)
    .readTimeout(5, TimeUnit.MINUTES)
    .build()
```

---

## 🚀 How to Use NetMock

1. Launch your Android application onto an active emulator or a physical device with USB debugging enabled.
2. Click the **NetMock Dashboard** tab on the right sidebar edge of Android Studio.
3. Your device serial identity should automatically populate in the **Target Device** dropdown. 
4. Click **"Start NetMock Bridge"** to open the cross-platform ADB connection pipeline.
5. Choose your target strategy:
    *   **Global**: Controls all outgoing HTTP flows.
    *   **REST**: Targets individual paths. Type an endpoint snippet (e.g., `/api/v1/profile`) or type `500` to inject error codes.
    *   **GraphQL**: Targets specific operations. Type a clean query signature name (e.g., `SubmitOrder`) to inject network error schemas.
6. Adjust the **Artificial Latency** dropdown block at any time. Changes are broadcast down the pipeline and applied to your running network client threads instantly!

---

## 🤝 Contributing & Feedback

Contributions, feature requests, and bug reports are highly welcome! Feel free to open an issue or submit a pull request if you want to expand NetMock's capabilities (such as adding response body rewriting or automated packet dropping).

Created with ❤️ by ** Rajesh Kunwar **
