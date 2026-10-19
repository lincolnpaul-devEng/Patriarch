package com.campus.network.core.model

/**
 * Campus-wide security access tiers enforced across navigation routes,
 * marketplace escrow transactions, and live GPS location sharing.
 */
enum class CampusAccessTier(val label: String, val badgeText: String) {
    UNAUTHENTICATED(
        label = "Unauthenticated",
        badgeText = "Login Required"
    ),
    GUEST_READ_ONLY(
        label = "Guest Preview Mode",
        badgeText = "Guest • Read Only"
    ),
    VERIFIED_STUDENT(
        label = "Verified Collegiate Student",
        badgeText = "Verified .ac.ke / .edu"
    ),
    GUILD_EXECUTIVE(
        label = "Campus Guild Executive & Arbitrator",
        badgeText = "Guild Executive"
    )
}

/**
 * Defines the active user's role perspective inside a Marketplace / SafePay transaction.
 * Controls what the Buyer sees vs. what the Seller sees.
 */
enum class MarketplaceRolePerspective(val label: String) {
    BUYER_BROWSING("Buyer View (Pre-Escrow)"),
    BUYER_ESCROW_LOCKED("Buyer View (Escrow Locked)"),
    SELLER_OWNER("Seller / Listing Owner View")
}

/**
 * Granular Location Privacy Scopes controlling WHO can view a student's live GPS coordinates
 * and WHO is blocked from seeing location data.
 */
enum class LocationPrivacyScope(
    val title: String,
    val subtitle: String,
    val badgeLabel: String,
    val allowsCoordinatesBroadcast: Boolean
) {
    ESCROW_COUNTERPARTY_ONLY(
        title = "SafePay Counterparty Only",
        subtitle = "Only the matched buyer or seller in your active escrow order can see your SafeZone handoff pin. Auto-revokes on QR release.",
        badgeLabel = "Escrow Match Only",
        allowsCoordinatesBroadcast = true
    ),
    VERIFIED_CAMPUS_PEERS(
        title = "Verified Campus Peers (.ac.ke)",
        subtitle = "Visible only to signed-in students with a verified university email on your campus. Guests and external links are blocked.",
        badgeLabel = "Verified Peers Only",
        allowsCoordinatesBroadcast = true
    ),
    TIMED_SESSION_30M(
        title = "30-Minute Ephemeral Link",
        subtitle = "Generates a signed one-time session token that automatically expires and revokes GPS access after 30 minutes.",
        badgeLabel = "30m Ephemeral Token",
        allowsCoordinatesBroadcast = true
    ),
    GHOST_MODE_HIDDEN(
        title = "Ghost Mode (No Location Sharing)",
        subtitle = "Your live GPS location is hidden from everyone on campus. Existing location links are suspended immediately.",
        badgeLabel = "Ghost Mode • Hidden",
        allowsCoordinatesBroadcast = false
    )
}

/**
 * Computed access control matrix for a specific Marketplace listing.
 */
data class MarketplaceAccessControl(
    val rolePerspective: MarketplaceRolePerspective,
    val isSellerOwner: Boolean,
    val canReserveOrBuy: Boolean,
    val canViewExactPickupCoordinates: Boolean,
    val canViewSellerSecretQrTicket: Boolean,
    val canOpenBuyerQrScanner: Boolean,
    val maskedOrVisibleReleaseCode: String,
    val pickupLocationSecurityNotice: String
)

/**
 * Computed decision for whether a viewer is permitted to see a peer's location.
 */
data class LocationAccessDecision(
    val isAllowed: Boolean,
    val scope: LocationPrivacyScope,
    val reason: String
)

/**
 * Represents an authorized or blocked viewer entry in the Location Privacy Audit sheet.
 */
data class LocationAudienceRule(
    val groupName: String,
    val accessDescription: String,
    val isAuthorized: Boolean
)

/**
 * Deterministic Security Policy Evaluator used across Navigation, Hub Marketplace, and Map.
 */
object CampusSecurityPolicy {

    private val PUBLIC_AUTH_ROUTES = setOf(
        "splash",
        "onboarding",
        "onboarding_transition",
        "auth",
        "verify",
        "onboarding_survey"
    )

    private val PRIVILEGED_WRITE_ROUTES = setOf(
        "go_live",
        "notification_settings"
    )

    fun resolveAccessTier(
        profile: StudentProfile?,
        isAuthenticatedSession: Boolean = true,
        isGuestSession: Boolean = false
    ): CampusAccessTier {
        if (!isAuthenticatedSession && !isGuestSession) {
            return CampusAccessTier.UNAUTHENTICATED
        }
        if (isGuestSession || profile == null) {
            return CampusAccessTier.GUEST_READ_ONLY
        }
        val isGuildExec = profile.studentId.startsWith("EXEC-", ignoreCase = true) ||
            profile.personaRole.contains("Executive", ignoreCase = true) ||
            profile.personaRole.contains("Guild", ignoreCase = true)
        return if (isGuildExec) {
            CampusAccessTier.GUILD_EXECUTIVE
        } else {
            val email = profile.universityEmail.trim().lowercase()
            val hasValidUniversityDomain = email.endsWith(".ac.ke") ||
                email.endsWith("strathmore.edu") ||
                profile.onboardingPreferences.isVerifiedStudentEmail
            if (hasValidUniversityDomain && profile.behavioralTrustScore >= 40) {
                CampusAccessTier.VERIFIED_STUDENT
            } else {
                CampusAccessTier.GUEST_READ_ONLY
            }
        }
    }

    fun canAccessRoute(route: String?, accessTier: CampusAccessTier): Boolean {
        if (route.isNullOrBlank()) return false
        val baseRoute = route.substringBefore("?").substringBefore("/")
        if (baseRoute in PUBLIC_AUTH_ROUTES) return true

        if (accessTier == CampusAccessTier.UNAUTHENTICATED) return false
        if (baseRoute in PRIVILEGED_WRITE_ROUTES && accessTier == CampusAccessTier.GUEST_READ_ONLY) {
            return false
        }
        return true
    }

    fun canPerformPrivilegedAction(accessTier: CampusAccessTier): Boolean {
        return accessTier == CampusAccessTier.VERIFIED_STUDENT ||
            accessTier == CampusAccessTier.GUILD_EXECUTIVE
    }

    /**
     * Masks the seller's secret 6-character release code for buyers so a buyer cannot
     * bypass physical inspection at the campus SafeZone.
     */
    fun maskReleaseCode(
        rawCode: String,
        isSellerOwner: Boolean,
        isRevealedBySeller: Boolean = false
    ): String {
        if (isSellerOwner && isRevealedBySeller) {
            return rawCode
        }
        val prefix = rawCode.substringBefore("-", "CP")
        return "$prefix-••••••"
    }

    /**
     * Evaluates what the current user is allowed to see and do on a Marketplace listing
     * based on whether they are the Seller Owner, a Browsing Buyer, or an Escrow-Locked Buyer.
     */
    fun evaluateMarketplacePermissions(
        item: MarketItem,
        currentUserName: String,
        isOrderPurchased: Boolean,
        forceSellerPerspective: Boolean = false,
        isSellerCodeRevealed: Boolean = false
    ): MarketplaceAccessControl {
        val normalizedUser = currentUserName.trim().lowercase()
        val normalizedSeller = item.sellerName.trim().lowercase()
        val isNaturalOwner = normalizedUser.isNotEmpty() &&
            (normalizedSeller == normalizedUser ||
                normalizedSeller.startsWith(normalizedUser.substringBefore(" ")) ||
                normalizedUser.startsWith(normalizedSeller.substringBefore(" ")))

        val isSellerOwner = forceSellerPerspective || isNaturalOwner

        val perspective = when {
            isSellerOwner -> MarketplaceRolePerspective.SELLER_OWNER
            isOrderPurchased -> MarketplaceRolePerspective.BUYER_ESCROW_LOCKED
            else -> MarketplaceRolePerspective.BUYER_BROWSING
        }

        val canViewExactCoords = isSellerOwner || isOrderPurchased
        val securityNotice = when (perspective) {
            MarketplaceRolePerspective.SELLER_OWNER ->
                "Seller Security View: Your exact handoff pin (${item.pickupSpotName}) is hidden from browsing buyers until they lock SafePay Escrow."
            MarketplaceRolePerspective.BUYER_ESCROW_LOCKED ->
                "Escrow Verified: Exact SafeZone handoff pin and live seller meetup coordinates are unlocked for your order."
            MarketplaceRolePerspective.BUYER_BROWSING ->
                "Location Protected: Exact GPS coordinates & dormitory bay are locked until SafePay Escrow is reserved."
        }

        return MarketplaceAccessControl(
            rolePerspective = perspective,
            isSellerOwner = isSellerOwner,
            canReserveOrBuy = !isSellerOwner && !isOrderPurchased,
            canViewExactPickupCoordinates = canViewExactCoords,
            canViewSellerSecretQrTicket = isSellerOwner,
            canOpenBuyerQrScanner = !isSellerOwner && isOrderPurchased,
            maskedOrVisibleReleaseCode = maskReleaseCode(
                rawCode = item.qrReleaseCode,
                isSellerOwner = isSellerOwner,
                isRevealedBySeller = isSellerCodeRevealed
            ),
            pickupLocationSecurityNotice = securityNotice
        )
    }

    /**
     * Evaluates who is authorized to see the user's location and who is blocked
     * for a selected [LocationPrivacyScope].
     */
    fun getAudienceRulesForScope(
        scope: LocationPrivacyScope,
        universityName: String = "Strathmore University"
    ): List<LocationAudienceRule> {
        return when (scope) {
            LocationPrivacyScope.ESCROW_COUNTERPARTY_ONLY -> listOf(
                LocationAudienceRule(
                    groupName = "Matched SafePay Buyer / Seller",
                    accessDescription = "Authorized during active handoff window at CCTV SafeZone",
                    isAuthorized = true
                ),
                LocationAudienceRule(
                    groupName = "Browsing Marketplace Users (Unpaid)",
                    accessDescription = "Blocked from exact GPS pin until funds are locked in escrow",
                    isAuthorized = false
                ),
                LocationAudienceRule(
                    groupName = "General Public & Unverified Guests",
                    accessDescription = "Blocked from viewing live coordinates or dormitory hall",
                    isAuthorized = false
                )
            )

            LocationPrivacyScope.VERIFIED_CAMPUS_PEERS -> listOf(
                LocationAudienceRule(
                    groupName = "Verified $universityName Peers (.ac.ke)",
                    accessDescription = "Authorized to view live study-jam & meetup pin",
                    isAuthorized = true
                ),
                LocationAudienceRule(
                    groupName = "Matched SafePay Counterparty",
                    accessDescription = "Authorized for SafeZone exchange",
                    isAuthorized = true
                ),
                LocationAudienceRule(
                    groupName = "Unverified Guests & External Links",
                    accessDescription = "Blocked by closed-loop domain guard",
                    isAuthorized = false
                )
            )

            LocationPrivacyScope.TIMED_SESSION_30M -> listOf(
                LocationAudienceRule(
                    groupName = "Invited Campus Friends (30-Min Token)",
                    accessDescription = "Authorized via signed session token; auto-revokes in 30 mins",
                    isAuthorized = true
                ),
                LocationAudienceRule(
                    groupName = "Expired or Forwarded Links",
                    accessDescription = "Blocked automatically after 30-minute TTL window",
                    isAuthorized = false
                )
            )

            LocationPrivacyScope.GHOST_MODE_HIDDEN -> listOf(
                LocationAudienceRule(
                    groupName = "All Campus Peers & Marketplace Users",
                    accessDescription = "Blocked — Ghost Mode active; live GPS telemetry is paused",
                    isAuthorized = false
                ),
                LocationAudienceRule(
                    groupName = "External Shared Links",
                    accessDescription = "Revoked — All previous share tokens invalidated",
                    isAuthorized = false
                )
            )
        }
    }
}
