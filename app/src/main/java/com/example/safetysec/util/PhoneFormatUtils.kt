package com.example.safetysec.util

import android.util.Log

/**
 * Utility object for handling phone number formatting and validation.
 * Ensures all phone numbers conform to E.164 format required by Firebase Authentication.
 *
 * E.164 Format: +[country code][subscriber number]
 * Example: +12125551234 (US), +4733334455 (Norway), +442071838750 (UK)
 */
object PhoneFormatUtils {

    private const val TAG = "PhoneFormatUtils"

    // Country code mappings
    private val countryCodeMap = mapOf(
        "US" to "1", "CA" to "1", // North America
        "GB" to "44", "IE" to "353", "PT" to "351", // UK/Ireland/Portugal
        "NO" to "47", "SE" to "46", "DK" to "45", "FI" to "358", // Scandinavia
        "DE" to "49", "FR" to "33", "ES" to "34", "IT" to "39", // Europe
        "AU" to "61", "NZ" to "64", // Oceania
        "BR" to "55", "MX" to "52", // Americas
        "JP" to "81", "CN" to "86", "IN" to "91", // Asia
        "ZA" to "27" // Africa
    )

    // Country code to expected digit count (after country code)
    private val countryDigitCounts = mapOf(
        "1" to listOf(10), // US/Canada: 10 digits
        "44" to listOf(10), // UK: 10 digits
        "351" to listOf(9), // Portugal: 9 digits
        "47" to listOf(8), // Norway: 8 digits
        "46" to listOf(8, 9), // Sweden: 8-9 digits
        "45" to listOf(8), // Denmark: 8 digits
        "358" to listOf(7, 8, 9), // Finland: 7-9 digits
        "49" to listOf(10, 11), // Germany: 10-11 digits
        "33" to listOf(9), // France: 9 digits
        "34" to listOf(9), // Spain: 9 digits
        "39" to listOf(9, 10), // Italy: 9-10 digits
        "61" to listOf(9), // Australia: 9 digits
        "64" to listOf(9, 10), // New Zealand: 9-10 digits
        "55" to listOf(10, 11), // Brazil: 10-11 digits
        "52" to listOf(10), // Mexico: 10 digits
        "81" to listOf(9, 10), // Japan: 9-10 digits
        "86" to listOf(10, 11), // China: 10-11 digits
        "91" to listOf(10), // India: 10 digits
        "27" to listOf(9) // South Africa: 9 digits
    )

    /**
     * Validates if a phone number is in valid E.164 format.
     *
     * @param phoneNumber The phone number to validate
     * @return true if valid E.164 format, false otherwise
     */
    fun isValidE164Format(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) {
            Log.w(TAG, "Phone number is null or blank")
            return false
        }

        val trimmed = phoneNumber.trim()

        // Must start with +
        if (!trimmed.startsWith("+")) {
            Log.w(TAG, "Phone number doesn't start with +: $trimmed")
            return false
        }

        // Must contain only + and digits
        if (!trimmed.matches(Regex("^\\+\\d+$"))) {
            Log.w(TAG, "Phone number contains invalid characters: $trimmed")
            return false
        }

        // Remove + for digit count
        val digitsOnly = trimmed.substring(1)

        // E.164 allows max 15 digits (not including +)
        if (digitsOnly.length > 15) {
            Log.w(TAG, "Phone number has too many digits (${digitsOnly.length}): $trimmed")
            return false
        }

        // Must have at least 7 digits (some countries have short numbers)
        if (digitsOnly.length < 7) {
            Log.w(TAG, "Phone number has too few digits (${digitsOnly.length}): $trimmed")
            return false
        }

        Log.d(TAG, "✓ Valid E.164 format: ${maskPhoneNumber(trimmed)}")
        return true
    }

    /**
     * Normalizes a phone number to E.164 format.
     * Removes spaces, dashes, parentheses, and adds country code if missing.
     *
     * @param phoneNumber The raw phone number
     * @param countryCode The country code prefix (e.g., "+1" for US, "+47" for Norway)
     * @return E.164 formatted phone number, or null if invalid
     */
    fun normalizePhoneNumber(phoneNumber: String?, countryCode: String = "+1"): String? {
        if (phoneNumber.isNullOrBlank()) {
            Log.w(TAG, "Cannot normalize null or blank phone number")
            return null
        }

        var cleaned = phoneNumber.trim()

        // Remove common formatting characters
        cleaned = cleaned
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")
            .replace("/", "")

        // If already has +, validate it's E.164
        if (cleaned.startsWith("+")) {
            return if (isValidE164Format(cleaned)) {
                Log.d(TAG, "Phone number already in E.164 format: ${maskPhoneNumber(cleaned)}")
                cleaned
            } else {
                Log.w(TAG, "Phone number starts with + but invalid format: ${maskPhoneNumber(cleaned)}")
                null
            }
        }

        // If starts with 00 (international format), replace with +
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2)
            return if (isValidE164Format(cleaned)) {
                Log.d(TAG, "Converted 00 prefix to E.164: ${maskPhoneNumber(cleaned)}")
                cleaned
            } else {
                Log.w(TAG, "Invalid after converting 00 prefix: ${maskPhoneNumber(cleaned)}")
                null
            }
        }

        // If doesn't start with +, assume country code needs to be added
        // Remove leading 0 if present (common in national format)
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }

        // Add country code
        val normalized = countryCode + cleaned

        return if (isValidE164Format(normalized)) {
            Log.d(TAG, "Normalized to E.164: ${maskPhoneNumber(normalized)}")
            normalized
        } else {
            Log.w(TAG, "Failed to normalize phone with country code $countryCode: ${maskPhoneNumber(normalized)}")
            null
        }
    }

    /**
     * Extracts the country code from a phone number in E.164 format.
     *
     * @param phoneNumber The E.164 formatted phone number
     * @return The country code (e.g., "1", "47", "44"), or null if not found
     */
    fun extractCountryCode(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank() || !phoneNumber.startsWith("+")) {
            return null
        }

        val digitsOnly = phoneNumber.substring(1)

        // Try to extract country code (1-3 digits)
        for (length in 3 downTo 1) {
            val possibleCode = digitsOnly.take(length)
            if (possibleCode.isNotEmpty()) {
                // Check if this is a known country code
                if (countryCodeMap.values.contains(possibleCode)) {
                    return possibleCode
                }
            }
        }

        return null
    }

    /**
     * Extracts the phone number without the country code.
     *
     * @param phoneNumber The E.164 formatted phone number
     * @return The phone number part without country code
     */
    fun extractPhoneWithoutCountryCode(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank() || !phoneNumber.startsWith("+")) {
            return null
        }

        val countryCode = extractCountryCode(phoneNumber) ?: return null
        return phoneNumber.substring(1 + countryCode.length)
    }

    /**
     * Gets the expected length of a phone number for a given country code.
     *
     * @param countryCode The country code (with or without +)
     * @return List of valid digit counts for this country, or empty list if unknown
     */
    fun getExpectedLengthsForCountryCode(countryCode: String): List<Int> {
        val cleanCode = countryCode.removePrefix("+")
        return countryDigitCounts[cleanCode] ?: emptyList()
    }

    /**
     * Gets country code for a given country code.
     *
     * @param countryCode The country code (e.g., "NO", "US", "GB")
     * @return The numeric country code (e.g., "47", "1", "44"), or null if not found
     */
    fun getCountryCodeByCountry(countryCode: String): String? {
        return countryCodeMap[countryCode.uppercase()]
    }

    /**
     * Masks phone number for logging (shows only last 4 digits).
     *
     * @param phoneNumber The phone number to mask
     * @return Masked phone number (e.g., "+****2190")
     */
    fun maskPhoneNumber(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) {
            return "****"
        }
        return if (phoneNumber.length > 4) {
            "+" + "*".repeat(phoneNumber.length - 5) + phoneNumber.takeLast(4)
        } else {
            "*".repeat(phoneNumber.length)
        }
    }

    /**
     * Validates a phone number and normalizes it to E.164 format in one step.
     *
     * @param phoneNumber The raw phone number
     * @param countryCode The country code (e.g., "+1", "+47")
     * @return E.164 formatted phone number if valid, null otherwise
     */
    fun validateAndNormalize(phoneNumber: String?, countryCode: String = "+1"): String? {
        return normalizePhoneNumber(phoneNumber, countryCode)?.also { normalized ->
            if (isValidE164Format(normalized)) {
                Log.d(TAG, "✓ Validation and normalization successful: ${maskPhoneNumber(normalized)}")
            } else {
                Log.e(TAG, "✗ Normalization produced invalid format: ${maskPhoneNumber(normalized)}")
            }
        }
    }

    /**
     * Common country codes for quick reference.
     */
    object CommonCountryCodes {
        const val US_CANADA = "+1"
        const val UK = "+44"
        const val IRELAND = "+353"
        const val PORTUGAL = "+351"
        const val NORWAY = "+47"
        const val SWEDEN = "+46"
        const val DENMARK = "+45"
        const val FINLAND = "+358"
        const val GERMANY = "+49"
        const val FRANCE = "+33"
        const val SPAIN = "+34"
        const val ITALY = "+39"
        const val AUSTRALIA = "+61"
        const val NEW_ZEALAND = "+64"
        const val BRAZIL = "+55"
        const val MEXICO = "+52"
        const val JAPAN = "+81"
        const val CHINA = "+86"
        const val INDIA = "+91"
        const val SOUTH_AFRICA = "+27"
    }
}