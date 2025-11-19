package com.kevann.nosteqTech



object MockData {
    val routers = listOf(
        Router(
            id = "1",
            customerName = "Alice Johnson",
            customerId = "CUST-1001",
            model = "Huawei EG8145V5",
            status = RouterStatus.LOS,
            lastSeen = "Offline for 2 hours",
            ipAddress = "192.168.100.45",
            macAddress = "A1:B2:C3:D4:E5:F6",
            rxPower = -28.5, // Critical
            txPower = 2.4,
            temperature = 45,
            address = "123 Maple Street, Springfield"
        ),
        Router(
            id = "2",
            customerName = "Bob Smith",
            customerId = "CUST-1002",
            model = "MikroTik hAP ac2",
            status = RouterStatus.LATENCY,
            lastSeen = "Online",
            ipAddress = "10.0.0.5",
            macAddress = "AA:BB:CC:DD:EE:FF",
            rxPower = -18.2,
            txPower = 3.1,
            temperature = 38,
            address = "456 Oak Avenue, Metropolis",
            latencyMs = 150
        ),
        Router(
            id = "3",
            customerName = "Carol White",
            customerId = "CUST-1003",
            model = "Nokia G-2425G-A",
            status = RouterStatus.OFFLINE,
            lastSeen = "Offline for 15 mins",
            ipAddress = "192.168.1.10",
            macAddress = "11:22:33:44:55:66",
            rxPower = -99.0, // No signal
            txPower = 0.0,
            temperature = 25,
            address = "789 Pine Lane, Gotham"
        ),
        Router(
            id = "4",
            customerName = "David Brown",
            customerId = "CUST-1004",
            model = "Huawei EG8145V5",
            status = RouterStatus.LOS,
            lastSeen = "Offline for 5 hours",
            ipAddress = "192.168.100.50",
            macAddress = "99:88:77:66:55:44",
            rxPower = -32.0, // Very Critical
            txPower = 2.1,
            temperature = 42,
            address = "321 Elm Street, Smallville"
        ),
        Router(
            id = "5",
            customerName = "Eve Davis",
            customerId = "CUST-1005",
            model = "ZTE F670L",
            status = RouterStatus.ONLINE,
            lastSeen = "Online",
            ipAddress = "192.168.1.20",
            macAddress = "12:34:56:78:90:AB",
            rxPower = -19.5,
            txPower = 2.8,
            temperature = 35,
            address = "654 Cedar Blvd, Star City"
        )
    )

    fun getRouterById(id: String): Router? {
        return routers.find { it.id == id }
    }
}
