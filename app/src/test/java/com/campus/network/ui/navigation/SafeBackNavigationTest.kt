package com.campus.network.ui.navigation

import com.campus.network.core.ui.notifications.CampusNotificationDeepLink
import com.campus.network.ui.resolveNotificationDeepLinkRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeBackNavigationTest {

    @Test
    fun normalBack_withValidPreviousHub_popsBackStack() {
        val action = resolveSafeBackAction(
            fromBaseRoute = Screen.Market.route,
            currentRoute = Screen.Market.route,
            previousRoute = Screen.Hub.route,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.POP_BACK_STACK, action)
    }

    @Test
    fun parameterizedMarketRoute_withItemIdQuery_popsBackToHub() {
        val action = resolveSafeBackAction(
            fromBaseRoute = Screen.Market.route,
            currentRoute = "market?itemId=item_12345",
            previousRoute = Screen.Hub.route,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.POP_BACK_STACK, action)
    }

    @Test
    fun notificationDeepLinkRouting_resolvesLiveStreamAndMarketAndAllPaths() {
        assertEquals(
            "campus_live_feed?storyId=stream_abc",
            resolveNotificationDeepLinkRoute(CampusNotificationDeepLink.OpenLiveStream("stream_abc"))
        )
        assertEquals(
            "campus_live_feed?storyId=founders",
            resolveNotificationDeepLinkRoute(CampusNotificationDeepLink.OpenLiveStream(null))
        )
        assertEquals(
            "market?itemId=item_macbook_42",
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenSafePayMarketplace(itemId = "item_macbook_42", orderRef = null)
            )
        )
        assertEquals(
            "market?itemId=",
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenSafePayMarketplace(itemId = null, orderRef = "ORD-1")
            )
        )
        assertEquals(
            Screen.Messages.route,
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenDirectChat(threadId = "peer_1", peerName = "Brian Otieno")
            )
        )
        assertEquals(
            Screen.Messages.route,
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenIncomingCall(
                    peerId = "peer_2",
                    peerName = "Faith Wangari",
                    isVideo = true,
                    callSessionId = "call_99"
                )
            )
        )
        assertEquals(
            Screen.Map.route,
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenFlashMeetOnMap(-1.28, 36.82, "Study Sprint")
            )
        )
        assertEquals(
            Screen.Map.route,
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenSharedLocation("tok_123")
            )
        )
        assertEquals(
            Screen.Channels.route,
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenGuildChannels("chan_1")
            )
        )
        assertEquals(
            Screen.Notifications.route,
            resolveNotificationDeepLinkRoute(
                CampusNotificationDeepLink.OpenFriendRequest("peer_3", "Kevin Kamau", null)
            )
        )
    }

    @Test
    fun duplicateTapDuringFadeOut_whenCurrentRouteAlreadyChanged_isIgnored() {
        val action = resolveSafeBackAction(
            fromBaseRoute = Screen.Market.route,
            currentRoute = Screen.Hub.route,
            previousRoute = null,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.IGNORE_STALE, action)
    }

    @Test
    fun coldLaunchDeepLink_withSplashUnderneath_navigatesToFallback() {
        val action = resolveSafeBackAction(
            fromBaseRoute = Screen.Market.route,
            currentRoute = Screen.Market.route,
            previousRoute = Screen.Splash.route,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.NAVIGATE_FALLBACK, action)
    }

    @Test
    fun emptyUnderStack_whenPreviousRouteNull_navigatesToFallback() {
        val action = resolveSafeBackAction(
            fromBaseRoute = Screen.Market.route,
            currentRoute = Screen.Market.route,
            previousRoute = null,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.NAVIGATE_FALLBACK, action)
    }

    @Test
    fun verifyScreen_withAuthUnderneath_popsBackToAuth_andIgnoresStaleDoubleTap() {
        val backToAuth = resolveSafeBackAction(
            fromBaseRoute = "verify",
            currentRoute = "verify?target=a&isPhone=false",
            previousRoute = Screen.Auth.route,
            fallbackRoute = Screen.Auth.route
        )
        assertEquals(SafeBackAction.POP_BACK_STACK, backToAuth)

        val staleDoubleTap = resolveSafeBackAction(
            fromBaseRoute = "verify",
            currentRoute = Screen.Auth.route,
            previousRoute = null,
            fallbackRoute = Screen.Auth.route
        )
        assertEquals(SafeBackAction.IGNORE_STALE, staleDoubleTap)
    }

    @Test
    fun authenticatedOverlay_withStaleAuthUnderneath_navigatesToHubFallback() {
        val action = resolveSafeBackAction(
            fromBaseRoute = Screen.Notifications.route,
            currentRoute = Screen.Notifications.route,
            previousRoute = Screen.Auth.route,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.NAVIGATE_FALLBACK, action)
    }

    @Test
    fun parameterizedLiveFeedRoute_withHubUnderneath_popsBackStack() {
        val action = resolveSafeBackAction(
            fromBaseRoute = "campus_live_feed",
            currentRoute = "campus_live_feed?storyId=founders",
            previousRoute = Screen.Hub.route,
            fallbackRoute = Screen.Hub.route
        )
        assertEquals(SafeBackAction.POP_BACK_STACK, action)
    }
}
