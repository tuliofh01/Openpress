# Openpress — Stripe Payment Integration

> **Prerequisites:**
> - A [Stripe account](https://dashboard.stripe.com/register)
> - Test mode API keys from the Stripe Dashboard
> - HTTPS-enabled server (required by Stripe webhooks in production)
> **Status:** 🚧 Planned — Stripe integration has not been implemented yet. The `stripe-java` dependency, `StripeConfig.kt`, `PaymentRoutes.kt`, webhook handler, and `stripe:` config section in `application.yaml` described below are all pending. This document serves as an implementation guide.

---

## 1. Add the Stripe SDK Dependency

```kotlin
// build.gradle.kts
dependencies {
    // ... existing dependencies
    implementation("com.stripe:stripe-java:22.3.0")
}
```

Then sync Gradle:

```bash
./gradlew build --refresh-dependencies
```

---

## 2. Configure API Keys

### 2.1 Application Config

```yaml
# application.yaml
ktor:
  deployment:
    port: 8080

stripe:
  secret_key: ${STRIPE_SECRET_KEY}     # Environment variable
  publishable_key: ${STRIPE_PUBLISHABLE_KEY}
  webhook_secret: ${STRIPE_WEBHOOK_SECRET}
```

### 2.2 Environment Variables

```bash
export STRIPE_SECRET_KEY="sk_test_..."
export STRIPE_PUBLISHABLE_KEY="pk_test_..."
export STRIPE_WEBHOOK_SECRET="whsec_..."
```

---

## 3. Stripe Configuration Module

```kotlin
// src/main/kotlin/StripeConfig.kt
package org.nexus.openpress

import io.ktor.server.application.*
import com.stripe.Stripe

fun Application.configureStripe() {
    val secretKey = environment.config.property("stripe.secret_key").getString()
    Stripe.apiKey = secretKey
}
```

Register in `application.yaml`:

```yaml
ktor:
  application:
    modules:
      # ... other modules ...
      - org.nexus.openpress.StripeConfigKt.configureStripe
```

---

## 4. Payment Intent Routes

```kotlin
// src/main/kotlin/routes/PaymentRoutes.kt
package org.nexus.openpress

import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePaymentRoutes() {
    routing {

        // ── Create a Payment Intent ──────────────────────
        post("/payments/create-intent") {
            val request = call.receive<CreatePaymentIntentRequest>()

            val params = PaymentIntentCreateParams.builder()
                .setAmount(request.amount)    // Amount in cents ($10.00 = 1000)
                .setCurrency(request.currency)
                .putMetadata("order_id", request.orderId.toString())
                .build()

            val paymentIntent = PaymentIntent.create(params)

            call.respond(
                HttpStatusCode.Created,
                PaymentIntentResponse(paymentIntent.clientSecret)
            )
        }

        // ── Confirm Payment (idempotency) ───────────────
        post("/payments/confirm") {
            val request = call.receive<ConfirmPaymentRequest>()

            val paymentIntent = PaymentIntent.retrieve(request.paymentIntentId)
            paymentIntent.confirm()

            call.respond(HttpStatusCode.OK, mapOf("status" to "confirmed"))
        }
    }
}

// ── Request / Response DTOs ─────────────────────────────
@Serializable
data class CreatePaymentIntentRequest(
    val amount: Long,         // In cents
    val currency: String,     // "usd", "eur", "brl"
    val orderId: Int
)

@Serializable
data class ConfirmPaymentRequest(
    val paymentIntentId: String
)

@Serializable
data class PaymentIntentResponse(
    val clientSecret: String
)
```

---

## 5. Webhook Handler

Stripe sends events to your webhook endpoint when payment status changes.

```kotlin
// Inside configurePaymentRoutes()
post("/stripe/webhook") {
    val payload = call.receiveText()
    val sigHeader = call.request.headers["Stripe-Signature"]

    try {
        val event = Webhook.constructEvent(
            payload,
            sigHeader,
            environment.config.property("stripe.webhook_secret").getString()
        )

        when (event.type) {
            "payment_intent.succeeded" -> {
                val paymentIntent = event.data.`object` as PaymentIntent
                val orderId = paymentIntent.metadata["order_id"]?.toInt()
                // Update order status in your database
                log.info("Payment succeeded for order $orderId")
            }
            "payment_intent.payment_failed" -> {
                log.warn("Payment failed: ${event.data.`object`}")
            }
        }

        call.respond(HttpStatusCode.OK)
    } catch (e: SignatureVerificationException) {
        call.respond(HttpStatusCode.BadRequest, "Invalid signature")
    }
}
```

---

## 6. Secure the Routes

Wrap payment routes in `authenticate("jwt")` to require authentication:

```kotlin
authenticate("jwt") {
    post("/payments/create-intent") { /* ... */ }
    post("/payments/confirm") { /* ... */ }
}

// Webhooks must be public (Stripe can't send JWT tokens)
post("/stripe/webhook") { /* ... */ }
```

---

## 7. Testing with Stripe CLI

```bash
# Install Stripe CLI (https://stripe.com/docs/stripe-cli)
stripe login
stripe listen --forward-to localhost:8080/stripe/webhook

# Trigger a test event
stripe trigger payment_intent.succeeded
```

---

## 8. Security Checklist

| Item | Status |
|------|--------|
| Stripe secret key loaded from environment variable | ✅ |
| Webhook signature verified | ✅ |
| HTTPS enabled in production | ✅ Required |
| Idempotency keys for retries | Consider adding |
| PCI compliance (Stripe handles card data via Elements) | ✅ Handled by Stripe |
| Rate limiting on payment endpoints | Consider adding |

---

## 9. Full Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation("com.stripe:stripe-java:22.3.0")
}
```
