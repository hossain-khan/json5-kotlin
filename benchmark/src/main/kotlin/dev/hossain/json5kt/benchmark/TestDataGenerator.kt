package dev.hossain.json5kt.benchmark

/**
 * Generates test data for benchmarking.
 */
object TestDataGenerator {
    fun createSimplePerson(): SimplePerson =
        SimplePerson(
            name = "John Doe",
            age = 30,
            isActive = true,
        )

    fun createComplexPerson(): ComplexPerson =
        ComplexPerson(
            firstName = "Jane",
            lastName = "Smith",
            age = 28,
            email = "jane.smith@example.com",
            isActive = true,
            salary = 75000.50,
            address =
                Address(
                    street = "123 Main St",
                    city = "San Francisco",
                    state = "CA",
                    zipCode = "94102",
                    country = "USA",
                ),
            phoneNumbers = listOf("+1-555-123-4567", "+1-555-987-6543"),
            skills = listOf("Kotlin", "Java", "Python", "JavaScript", "SQL"),
            metadata =
                mapOf(
                    "department" to "Engineering",
                    "level" to "Senior",
                    "team" to "Backend",
                ),
        )

    fun createCompany(): Company {
        val employees =
            (1..10).map { i ->
                ComplexPerson(
                    firstName = "Employee$i",
                    lastName = "Last$i",
                    age = 25 + i,
                    email = "employee$i@company.com",
                    isActive = i % 2 == 0,
                    salary = 50000.0 + (i * 5000),
                    address =
                        Address(
                            street = "$i Main St",
                            city = "City$i",
                            state = "ST",
                            zipCode = "1000$i",
                            country = "USA",
                        ),
                    phoneNumbers = listOf("+1-555-000-000$i"),
                    skills = listOf("Skill$i", "Skill${i + 1}"),
                    metadata = mapOf("id" to i.toString()),
                )
            }

        val departments =
            listOf(
                Department(
                    name = "Engineering",
                    manager = createSimplePerson(),
                    budget = 1000000.0,
                    projects = listOf("Project A", "Project B", "Project C"),
                ),
                Department(
                    name = "Marketing",
                    manager = SimplePerson("Mary Johnson", 35, true),
                    budget = 500000.0,
                    projects = listOf("Campaign X", "Campaign Y"),
                ),
                Department(
                    name = "Sales",
                    manager = SimplePerson("Bob Wilson", 40, true),
                    budget = 750000.0,
                    projects = listOf("Q1 Sales", "Q2 Sales", "Q3 Sales", "Q4 Sales"),
                ),
            )

        return Company(
            name = "TechCorp Inc.",
            employees = employees,
            departments = departments,
            founded = 2010,
            revenue = 50000000L,
            isPublic = true,
        )
    }

    fun createNumberTypes(): NumberTypes =
        NumberTypes(
            intValue = 42,
            longValue = 1234567890123L,
            doubleValue = 3.14159265359,
            floatValue = 2.71828f,
            byteValue = 127,
            shortValue = 32767,
        )

    fun createCollectionTypes(): CollectionTypes =
        CollectionTypes(
            stringList = listOf("apple", "banana", "cherry", "date", "elderberry"),
            intList = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            booleanList = listOf(true, false, true, false, true),
            nestedList =
                listOf(
                    listOf("a", "b", "c"),
                    listOf("d", "e", "f"),
                    listOf("g", "h", "i"),
                ),
            stringMap =
                mapOf(
                    "key1" to "value1",
                    "key2" to "value2",
                    "key3" to "value3",
                ),
            intMap =
                mapOf(
                    "one" to 1,
                    "two" to 2,
                    "three" to 3,
                ),
            nestedMap =
                mapOf(
                    "config" to mapOf("debug" to "true", "version" to "1.0"),
                    "settings" to mapOf("theme" to "dark", "lang" to "en"),
                ),
        )

    /**
     * Creates a list of simple persons for bulk testing.
     */
    fun createSimplePersonList(count: Int): List<SimplePerson> =
        (1..count).map { i ->
            SimplePerson(
                name = "Person $i",
                age = 20 + (i % 50),
                isActive = i % 2 == 0,
            )
        }

    /**
     * Creates a list of complex persons for bulk testing.
     */
    fun createComplexPersonList(count: Int): List<ComplexPerson> =
        (1..count).map { i ->
            ComplexPerson(
                firstName = "First$i",
                lastName = "Last$i",
                age = 20 + (i % 50),
                email = "person$i@example.com",
                isActive = i % 3 != 0,
                salary = 40000.0 + (i * 1000),
                address =
                    Address(
                        street = "$i Street",
                        city = "City ${i % 10}",
                        state = "ST",
                        zipCode = "${10000 + i}",
                        country = "Country ${i % 5}",
                    ),
                phoneNumbers = listOf("+1-555-${String.format("%03d", i % 1000)}-${String.format("%04d", i)}"),
                skills = listOf("Skill${i % 10}", "Skill${(i + 1) % 10}"),
                metadata = mapOf("id" to i.toString(), "batch" to (i / 100).toString()),
            )
        }
}
