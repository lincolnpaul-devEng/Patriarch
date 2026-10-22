
# Use Supabase with Android Kotlin

Learn how to create a Supabase project, add some sample data to your database, and query the data from an Android Kotlin app.

## 1. Create a Supabase project

To start, you need a Supabase project.

Create a new Supabase project from [the Dashboard of any organization](https://supabase.com/dashboard/new/_) you belong to.

Note: Use [the Management API](https://supabase.com/docs/reference/api/v1-create-a-project) or ask [the MCP server](https://supabase.com/docs/guides/ai-tools/mcp#account-management) to create a new Supabase project.

## 2. Set up your database

When your Supabase project is up and running, create an `instruments` table with some sample data. Then set only the privileges each Postgres role needs, add [Row Level Security (RLS)](https://supabase.com/docs/guides/database/postgres/row-level-security) for enhanced security for database data by default, and create an RLS policy to make the data in the table publicly readable.

Do these steps within your project's dashboard by copying and running the snippet in your project's [SQL Editor](https://supabase.com/dashboard/project/_/sql/new).

Note: Save some steps by clicking here to prefill the SQL in the SQL Editor, and then clicking **Run**.

Note: You can use [the Management API](https://supabase.com/docs/reference/api/v1-run-a-query) or ask [the MCP server](https://supabase.com/docs/guides/ai-tools/mcp#database) to execute SQL queries.

```sql
-- Create the table
create table instruments (
  id bigint primary key generated always as identity,
  name text not null
);

-- Insert sample data into the table
insert into instruments (name)
values
  ('violin'),
  ('viola'),
  ('cello');

-- Grant the privileges the role needs, which is read access
grant select on public.instruments to anon;

-- Enable row level security for the table
alter table instruments enable row level security;

-- Create a policy to allow the anon role to read from the instruments table
create policy "public can read instruments"
on public.instruments
for select to anon
using (true);
```

Note: If you disabled the Data API during project setup, enable it in the [**Integrations > Data API**](https://supabase.com/dashboard/project/_/integrations/data_api/settings) section of the Dashboard and expose the specific tables or functions you want to access. To automatically grant access for new tables and functions in `public`, enable **Automatically expose new tables**.

## 3. Create an Android app with Android Studio

Select the **Android Studio > New > New Android Project** menu item.

## 4. Set up AI tooling (optional)

Supabase provides two ways to give AI tools context about your project: Agent Skills, which give your AI coding agent procedural knowledge, and the MCP server, which connects AI assistants to your Supabase project directly.

### Agent Skills

[Agent Skills](https://supabase.com/docs/guides/ai-tools/ai-skills) is a curated set of instructions that give your AI agent procedural knowledge about working with Supabase.

Install them so your AI coding agent can produce more accurate, reliable code using current Supabase patterns, such as authentication, server-side rendering, and database migrations, rather than relying solely on training data.

#### Installing Agent Skills

To install, run the following command in the root of your project:

```bash
npx skills add supabase/agent-skills
```

### Supabase MCP server

The Supabase MCP server connects AI assistants to Supabase, so they can inspect your schema and act on your projects on your behalf. Find out how to add it to your client in [the MCP docs](https://supabase.com/docs/guides/ai-tools/mcp).

## 5. Install dependencies

Open `build.gradle.kts` (app) file and add the serialization plugin, Ktor client, and Supabase client.

Replace the version placeholders `$kotlin_version` with the Kotlin version of the project, and `$supabase_version` and `$ktor_version` with the respective latest versions. For example, Kotlin `2.2.21`, supabase-kt BOM `3.5.0`, and Ktor `3.0.3`.

Note: If your project uses Kotlin 2.0 or newer with Jetpack Compose, declare the Compose compiler plugin in your root `build.gradle.kts`:

```kotlin
id("org.jetbrains.kotlin.plugin.compose") version "$kotlin_version" apply false
```

Then apply it in the app module, without the version and without `apply false`:

```kotlin
id("org.jetbrains.kotlin.plugin.compose")
```

Note: You can find the latest supabase-kt version [on GitHub](https://github.com/supabase-community/supabase-kt/releases) and Ktor [in the Ktor documentation](https://ktor.io/docs/welcome.html). Match your Kotlin version to the supabase-kt release requirements.

```kotlin
plugins {
  ...
  kotlin("plugin.serialization") version "$kotlin_version"
}
...
dependencies {
  ...
  implementation(platform("io.github.jan-tennert.supabase:bom:$supabase_version"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.ktor:ktor-client-android:$ktor_version")
}
```

## 6. Add internet access permission

Add the following line to the `AndroidManifest.xml` file under the `manifest` tag and outside the `application` tag.

```xml
...
<uses-permission android:name="android.permission.INTERNET" />
...
```

## 7. Initialize the Supabase client

You can create a Supabase client whenever you need to perform an API call.

For a quick example, create a client at the top of the `MainActivity.kt` file below the imports.

Replace the `supabaseUrl` and `supabaseKey` with your own, which you can get from the helper below, or [from the project **Connect** panel](https://supabase.com/dashboard/project/_?showConnect=true\&framework=androidkotlin\&connectTab=mobiles):

Open Connect panel

```kotlin
import io.github.jan.supabase.postgrest.from
import ...

val supabase = createSupabaseClient(
    supabaseUrl = "YOUR_SUPABASE_URL",
    supabaseKey = "YOUR_SUPABASE_PUBLISHABLE_KEY"
  ) {
    install(Postgrest)
}
...
```

### Get API details

To interact with data in database tables, you use the client libraries that wrap [the auto-generated Data API endpoints](https://supabase.com/docs/guides/api), authenticating using the Project URL and key from [the project **Connect** dialog](https://supabase.com/dashboard/project/_?showConnect=true\&connectTab=mobiles\&framework=androidkotlin).

Note: See [API keys](https://supabase.com/docs/guides/getting-started/api-keys) for a full explanation of all key types, their uses, and where to find them.

Note: This guide substitutes your project URL and key directly into the code above, rather than reading them from a `.env` file. Mobile apps don't get environment variables injected at runtime the way a bundler-based web app does. You'd need a build-time mechanism specific to your toolchain, such as `--dart-define-from-file` for Flutter, an `.xcconfig` file for iOS, or a `Gradle` `BuildConfig` field for Android. That's a good next step once you're past this quickstart, so your keys aren't committed to source control.

## 8. Create a data model for instruments

Create a serializable data class to represent the data from the database.

Add the following below the `createSupabaseClient` function in the `MainActivity.kt` file.

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Instrument(
    val id: Int,
    val name: String,
)
```

## 9. Query data from the app

Use `LaunchedEffect` to fetch data from the database and display it in a `LazyColumn`.

Replace the default `MainActivity` class with the following code.

Note: This example application makes a network request from the UI code. In production, you should use a `ViewModel` to separate the UI and data fetching logic.

Note: This snippet omits the app-specific theme wrapper that Android Studio generates (named after your project, e.g. `<YourAppName>Theme`), so it compiles regardless of what you named your project. Wrap the `Surface` in your generated theme composable from `ui/theme/Theme.kt` if you want your project's Material theme applied.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                InstrumentsList()
            }
        }
    }
}

@Composable
fun InstrumentsList() {
    var instruments by remember { mutableStateOf<List<Instrument>>(listOf()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                instruments = supabase.from("instruments")
                                  .select().decodeList<Instrument>()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    if (error != null) {
        Text(
            "Error loading instruments: $error",
            modifier = Modifier.padding(8.dp),
        )
        return
    }
    LazyColumn {
        items(
            instruments,
            key = { instrument -> instrument.id },
        ) { instrument ->
            Text(
                instrument.name,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
```

## 10. Start the app

Run the app on an emulator or a physical device by clicking the `Run app` button in Android Studio.

## Production requirements

The quickstart procedure in this guide optimizes for getting you to a working app, not for production.

Before you deploy:

- If your app reads or writes through the Data API, review your [Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security) policies. Any policy you added here is scoped to this quickstart's sample data, not to real user data.
- Set your Supabase credentials as environment variables on whatever platform you deploy to, rather than committing them to source control.
- Configure a [custom domain](https://supabase.com/docs/guides/platform/custom-domains) for your Supabase project once you're ready to go live.

## Next steps

- Learn how to build a complete user management app with authentication in the [Kotlin tutorial](https://supabase.com/docs/guides/getting-started/tutorials/with-kotlin)
- Explore the [supabase-kt](https://github.com/supabase-community/supabase-kt) library on GitHub
- [Insert more data](https://supabase.com/docs/guides/database/import-data) into your database
- Upload and serve static files using [Storage](https://supabase.com/docs/guides/storage)
