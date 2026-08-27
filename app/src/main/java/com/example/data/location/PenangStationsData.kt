package com.example.data.location

import com.example.data.model.PenangEmergencyContact
import com.example.data.model.PenangStation
import kotlin.math.*

object PenangStationsData {

    val STATIONS = listOf(
        PenangStation(
            id = "usm_minden",
            name = "USM Minden (Gelugor)",
            district = "Penang Island (Timur Laut)",
            latitude = 5.3582,
            longitude = 100.2974,
            isOfficialCAQM = true,
            landmark = "Universiti Sains Malaysia, Gelugor"
        ),
        PenangStation(
            id = "balik_pulau",
            name = "Balik Pulau",
            district = "Penang Island (Barat Daya)",
            latitude = 5.3522,
            longitude = 100.2370,
            isOfficialCAQM = true,
            landmark = "Politeknik Balik Pulau & Durian Valley"
        ),
        PenangStation(
            id = "seberang_jaya",
            name = "Seberang Jaya",
            district = "Penang Mainland (Seberang Perai Tengah)",
            latitude = 5.3970,
            longitude = 100.4000,
            isOfficialCAQM = true,
            landmark = "Sunway Carnival & Hospital Seberang Jaya"
        ),
        PenangStation(
            id = "prai_industrial",
            name = "Prai Industrial Zone",
            district = "Penang Mainland (Seberang Perai)",
            latitude = 5.3789,
            longitude = 100.3850,
            isOfficialCAQM = true,
            landmark = "Penang Bridge Toll & Megamall Prai"
        ),
        PenangStation(
            id = "george_town",
            name = "George Town (Komtar & Gurney)",
            district = "Penang Island (Timur Laut)",
            latitude = 5.4164,
            longitude = 100.3327,
            isOfficialCAQM = true,
            landmark = "Komtar Tower & Gurney Drive Promenade"
        ),
        PenangStation(
            id = "bayan_lepas",
            name = "Bayan Lepas (FIZ & Airport)",
            district = "Penang Island (Barat Daya)",
            latitude = 5.2975,
            longitude = 100.2745,
            isOfficialCAQM = true,
            landmark = "Penang Int'l Airport & FIZ Tech Park"
        ),
        PenangStation(
            id = "butterworth",
            name = "Butterworth (Bagan)",
            district = "Penang Mainland (Seberang Perai Utara)",
            latitude = 5.3991,
            longitude = 100.3638,
            isOfficialCAQM = true,
            landmark = "Penang Sentral & Ferry Terminal"
        ),
        PenangStation(
            id = "tanjung_bungah",
            name = "Tanjung Bungah / Ferringhi",
            district = "Penang Island (Timur Laut)",
            latitude = 5.4650,
            longitude = 100.2800,
            isOfficialCAQM = false,
            landmark = "Floating Mosque & Batu Ferringhi Beach"
        ),
        PenangStation(
            id = "batu_kawan",
            name = "Batu Kawan (Eco Horizon)",
            district = "Penang Mainland (Seberang Perai Selatan)",
            latitude = 5.2635,
            longitude = 100.4358,
            isOfficialCAQM = true,
            landmark = "Sultan Abdul Halim Muadzam Shah Bridge & IKEA"
        ),
        PenangStation(
            id = "penang_hill",
            name = "Penang Hill / Air Itam",
            district = "Penang Island (Timur Laut)",
            latitude = 5.4042,
            longitude = 100.2764,
            isOfficialCAQM = false,
            landmark = "Penang Hill Funicular (833m altitude) & Kek Lok Si"
        ),
        PenangStation(
            id = "nibong_tebal",
            name = "Nibong Tebal / Jawi",
            district = "Penang Mainland (Seberang Perai Selatan)",
            latitude = 5.1698,
            longitude = 100.4789,
            isOfficialCAQM = false,
            landmark = "USM Engineering Campus & Bukit Panchor"
        )
    )

    val EMERGENCY_CONTACTS = listOf(
        PenangEmergencyContact(
            hospitalName = "Hospital Pulau Pinang (General Hospital)",
            location = "Jalan Residensi, George Town",
            phoneNumber = "+6042225333",
            hotlineName = "24H Emergency Respiratory Care",
            distanceEstimate = "Central George Town"
        ),
        PenangEmergencyContact(
            hospitalName = "Hospital Seberang Jaya",
            location = "Jalan Tun Hussein Onn, Seberang Jaya",
            phoneNumber = "+6043827333",
            hotlineName = "Mainland Trauma & Pulmonary Center",
            distanceEstimate = "Central Mainland"
        ),
        PenangEmergencyContact(
            hospitalName = "Hospital Balik Pulau",
            location = "Jalan Balik Pulau, Balik Pulau",
            phoneNumber = "+6048669333",
            hotlineName = "South-West Island Emergency",
            distanceEstimate = "West Penang Island"
        ),
        PenangEmergencyContact(
            hospitalName = "Bagan Specialist Centre",
            location = "Jalan Bagan 1, Butterworth",
            phoneNumber = "+6043405000",
            hotlineName = "Pulmonology & Acute Care Unit",
            distanceEstimate = "North Mainland"
        ),
        PenangEmergencyContact(
            hospitalName = "Pantai Hospital Penang",
            location = "Jalan Tengah, Bayan Baru",
            phoneNumber = "+6046433433",
            hotlineName = "South Island 24/7 Emergency",
            distanceEstimate = "Bayan Lepas / FIZ"
        ),
        PenangEmergencyContact(
            hospitalName = "Malaysia Emergency Services (999)",
            location = "National Emergency Dispatch",
            phoneNumber = "999",
            hotlineName = "Ambulance / Bomba Haze Emergency",
            distanceEstimate = "Immediate"
        )
    )

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }

    fun findNearestStation(lat: Double, lon: Double): Pair<PenangStation, Double> {
        var minDistance = Double.MAX_VALUE
        var closest = STATIONS[0]

        for (station in STATIONS) {
            val dist = calculateDistanceKm(lat, lon, station.latitude, station.longitude)
            if (dist < minDistance) {
                minDistance = dist
                closest = station
            }
        }
        return Pair(closest, minDistance)
    }
}
