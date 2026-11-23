package ch.opum.tricktrack.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One shared OkHttpClient for the whole app, so all requests reuse the same connection pool
 * and thread pool. Callers that need different timeouts should use [AppHttpClient.client].newBuilder(),
 * which keeps sharing those pools.
 */
object AppHttpClient {
    // Public OSM services (e.g. photon.komoot.io) reject OkHttp's default "okhttp/x.y" User-Agent
    // with HTTP 403 and ask clients to identify themselves.
    private const val USER_AGENT = "TrickTrack (Android; +https://github.com/OPUM-LABS/TrickTrack)"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                )
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
