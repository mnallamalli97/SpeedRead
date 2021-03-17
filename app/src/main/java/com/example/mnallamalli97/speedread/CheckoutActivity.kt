package com.example.mnallamalli97.speedread

import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.example.mnallamalli97.speedread.R.layout
import com.example.mnallamalli97.speedread.models.User
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.stripe.android.ApiResultCallback
import com.stripe.android.GooglePayConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CheckoutActivity : AppCompatActivity() {
  private var prePurchasebookTitle: TextView? = null
  private var prePurchaseBookAuthor: TextView? = null
  private var prePurchaseBookCover: ImageView? = null
  private var readSummaryButton: Button? = null
  private var readNowButton: Button? = null
  private var booksDatabaseReference: DatabaseReference? = null
  private var usersDatabaseReference: DatabaseReference? = null
  private var googlePayButton: RelativeLayout? = null
  private var isDarkModeOn = false
  private var bookPrice: Float? = null
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  private var paymentSuccess: Boolean = false
  private var bookId: Int? = null
  private var selectedBook: Book? = null
  private lateinit var auth: FirebaseAuth
  private var firebaseUser: FirebaseUser? = null
  private var currentUser: User? = null
  private var userPurchasesMapping: ArrayList<Boolean>? = null
  private var bookChaptersName: ArrayList<String>? = null
  private var bookChaptersPath: ArrayList<String>? = null
  var bookSummaryPath: String? = ""
  var author: String? = ""
  var title: String? = ""
  var featuredCover: String? = ""
  private val LOAD_PAYMENT_DATA_REQUEST_CODE = 1214
  private val stripe: Stripe by lazy {
    Stripe(this, PUBLISHABLE_KEY)
  }

  private val paymentsClient: PaymentsClient by lazy {
    Wallet.getPaymentsClient(
        this,
        Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    pref = applicationContext.getSharedPreferences("MyPref", 0) // 0 - for private mode
    editor = pref!!.edit()
    isDarkModeOn = pref!!.getBoolean("isDarkModeOn", false)
    if (isDarkModeOn) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
    super.onCreate(savedInstanceState)
    setContentView(layout.pre_purchase_activity)

    prePurchaseBookAuthor = findViewById(R.id.prePurchaseBookAuthor)
    prePurchasebookTitle = findViewById(R.id.prePurchasebookTitle)
    val book_layout_file = findViewById<View>(R.id.prePurchaseBookCover)
    prePurchaseBookCover = book_layout_file.findViewById(R.id.bookCoverImageButton)
    readSummaryButton = findViewById(R.id.readSummaryButton)
    readNowButton = findViewById(R.id.readNowButton)
    googlePayButton = findViewById(R.id.googlePayButton)

    val extras = intent.extras!!
    bookId = extras.getInt("id")
    val bookPath = extras.getString("book_path")
    bookSummaryPath = extras.getString("book_summary_path")
    author = extras.getString("author")
    title = extras.getString("title")
    val bookCoverPath = extras.getString("cover")
    bookPrice = extras.getFloat("book_price")
    bookChaptersName = extras.getStringArrayList("bookChaptersName")
    bookChaptersPath = extras.getStringArrayList("bookChaptersPath")
    featuredCover = extras.getString("featuredCover")


    selectedBook = Book(
        id = bookId,
        title = title,
        author = author,
        bookChaptersNames = bookChaptersName,
        bookChaptersPaths = bookChaptersPath,
        bookCover = bookCoverPath,
        featuredBookCover = featuredCover,
        bookSummaryPath = bookSummaryPath,
        bookPath = bookPath,
        bookPrice = bookPrice
    )

    retrieve()
    /*
      Sign up the user anonymously using Firebase
   */
    signInAndUpdateDatabase()


    // Initialize a Google Pay API client for an environment suitable for testing.
    // It's recommended to create the PaymentsClient object inside of the onCreate method.
    PaymentConfiguration.init(this, PUBLISHABLE_KEY)
    isReadyToPay()

    googlePayButton!!.setOnClickListener { payWithGoogle() }

    prePurchaseBookAuthor!!.text = author
    prePurchasebookTitle!!.text = title
    val url = bookCoverPath
    Glide.with(applicationContext)
        .load(url)
        .error(R.drawable.ic_library_books_24px)
        .into(prePurchaseBookCover!!)
    prePurchaseBookCover!!.isEnabled = false

    readNowButton!!.setOnClickListener {
      selectAndLoadChapter(selectedBook!!, readNowButton)
    }

    readSummaryButton!!.setOnClickListener {
      selectAndReadSummary()
    }
  }

  private fun signInAndUpdateDatabase() {
    usersDatabaseReference = FirebaseDatabase.getInstance()
        .getReference("speedread")
        .child("users")



    auth = Firebase.auth
    auth.signInAnonymously()
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            Toast.makeText(baseContext, "Authentication success.", Toast.LENGTH_SHORT)
                .show()
            firebaseUser = auth.currentUser

            // check if in the data base
            val currentUserReference = usersDatabaseReference!!.child("user${firebaseUser!!.uid}")

            currentUserReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
              override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userId = dataSnapshot.child("id").value as String?
                val purchasedMap = dataSnapshot.child("purchasedMap").value as ArrayList<Boolean>?


                if (userId == firebaseUser!!.uid) {
                  userPurchasesMapping = purchasedMap
                  currentUser = User(userId, purchasedMap)
                } else {
                  userPurchasesMapping!!.add(-1, false)
                  usersDatabaseReference!!.child("user${firebaseUser!!.uid}")
                      .setValue(User(firebaseUser!!.uid, userPurchasesMapping))
                }



                if (currentUser!!.isPurchasedMap?.get(bookId!!) == true) {
                  googlePayButton!!.visibility = View.GONE
                  readNowButton!!.visibility = View.VISIBLE

                } else {
                  googlePayButton!!.visibility = View.VISIBLE
                  readNowButton!!.visibility = View.GONE
                }

              }

                  override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(ContentValues.TAG, databaseError.message) // Don't ignore errors!
                  }
                })

          } else {
            // If sign in fails, display a message to the user.
            Log.w("sign in user", "signInAnonymously:failure", task.exception)
            Toast.makeText(
                baseContext, "Authentication failed.",
                Toast.LENGTH_SHORT
            )
                .show()
          }
        }
  }

  private fun selectAndLoadChapter(
    currentBook: Book,
    anchor: View?
  ) {
    val popupMenu = PopupMenu(this@CheckoutActivity, anchor!!)
    val menu = popupMenu.menu
    val numberOfChapters = currentBook.bookChaptersName?.size ?: 0

    for (i in 0 until numberOfChapters) {
      menu.add(i, i, i, currentBook.bookChaptersName?.get(i))
    }

    popupMenu.setOnMenuItemClickListener { item ->

      val chapterPath = currentBook.bookChaptersPath?.get(item.itemId)
      val intent = Intent(this@CheckoutActivity, SettingsActivity::class.java)

      intent.putExtra("title", currentBook.title)
      intent.putExtra("book_path", chapterPath)
      intent.putExtra("id", currentBook.id)
      intent.putExtra("author", currentBook.author)
      intent.putExtra("cover", currentBook.bookCover)
      intent.putExtra("book_price", currentBook.bookPrice)
      intent.putExtra("bookChaptersName", currentBook.bookChaptersName)
      intent.putExtra("bookChaptersPath", currentBook.bookSummaryPath)
      intent.putExtra("featuredCover", currentBook.featuredBookCover)

      startActivity(intent)
      true
    }
    popupMenu.show()
  }

  private fun selectAndReadSummary() {
    val intent = Intent(this@CheckoutActivity, SettingsActivity::class.java)

    intent.putExtra("title", title + ": Summary")
    intent.putExtra("author", author)
    intent.putExtra("book_summary_path", bookSummaryPath)
    startActivity(intent)
  }

  private fun isReadyToPay() {
    paymentsClient.isReadyToPay(createIsReadyToPayRequest())
        .addOnCompleteListener { task ->
          try {
            if (task.isSuccessful) {
              // show Google Pay as payment option
              googlePayButton!!.visibility = View.VISIBLE
            } else {
              // hide Google Pay as payment option
              Toast.makeText(
                  this,
                  "Unfortunately, Google Pay is not available on this device",
                  Toast.LENGTH_LONG
              )
                  .show();
            }
          } catch (exception: ApiException) {
          }
        }
  }

  /**
   * See https://developers.google.com/pay/api/android/reference/request-objects#example
   * for an example of the generated JSON.
   */
  private fun createIsReadyToPayRequest(): IsReadyToPayRequest {
    return IsReadyToPayRequest.fromJson(
        JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put(
                "allowedPaymentMethods",
                JSONArray().put(
                    JSONObject()
                        .put("type", "CARD")
                        .put(
                            "parameters",
                            JSONObject()
                                .put(
                                    "allowedAuthMethods",
                                    JSONArray()
                                        .put("PAN_ONLY")
                                        .put("CRYPTOGRAM_3DS")
                                )
                                .put(
                                    "allowedCardNetworks",
                                    JSONArray()
                                        .put("AMEX")
                                        .put("DISCOVER")
                                        .put("MASTERCARD")
                                        .put("VISA")
                                )
                        )
                )
            )
            .toString()
    )
  }

  private fun createPaymentDataRequest(): PaymentDataRequest {
    val cardPaymentMethod = JSONObject()
        .put("type", "CARD")
        .put(
            "parameters",
            JSONObject()
                .put(
                    "allowedAuthMethods", JSONArray()
                    .put("PAN_ONLY")
                    .put("CRYPTOGRAM_3DS")
                )
                .put(
                    "allowedCardNetworks",
                    JSONArray()
                        .put("AMEX")
                        .put("DISCOVER")
                        .put("MASTERCARD")
                        .put("VISA")
                )

                // require billing address
                .put("billingAddressRequired", true)
                .put(
                    "billingAddressParameters",
                    JSONObject()
                        // require full billing address
                        .put("format", "MIN")

                        // require phone number
                        .put("phoneNumberRequired", true)
                )
        )
        .put(
            "tokenizationSpecification",
            GooglePayConfig(this).tokenizationSpecification
        )

    // create PaymentDataRequest
    val paymentDataRequest = JSONObject()
        .put("apiVersion", 2)
        .put("apiVersionMinor", 0)
        .put(
            "allowedPaymentMethods",
            JSONArray().put(cardPaymentMethod)
        )
        .put(
            "transactionInfo", JSONObject()
            .put("totalPrice", "10.00")
            .put("totalPriceStatus", "FINAL")
            .put("currencyCode", "USD")
        )
        .put(
            "merchantInfo", JSONObject()
            .put("merchantName", "Stripe")
        )

        // require email address
        .put("emailRequired", true)
        .toString()

    return PaymentDataRequest.fromJson(paymentDataRequest)
  }

  private fun payWithGoogle() {
    AutoResolveHelper.resolveTask(
        paymentsClient.loadPaymentData(createPaymentDataRequest()),
        this@CheckoutActivity,
        LOAD_PAYMENT_DATA_REQUEST_CODE
    )
  }

  /**
   * Handle a resolved activity from the Google Pay payment sheet.
   *
   * @param requestCode Request code originally supplied to AutoResolveHelper in requestPayment().
   * @param resultCode Result code returned by the Google Pay API.
   * @param data Intent from the Google Pay API containing payment or error data.
   * @see [Getting a result
   * from an Activity](https://developer.android.com/training/basics/intents/result)
   */
  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      // Value passed in AutoResolveHelper
      LOAD_PAYMENT_DATA_REQUEST_CODE -> {
        when (resultCode) {
          RESULT_OK ->
            if (data != null) {
              googlePayButton!!.visibility = View.GONE
              readNowButton!!.visibility = View.VISIBLE
              onGooglePayResult(data)
            }

          RESULT_CANCELED -> {
            // The user cancelled the payment attempt
          }

          AutoResolveHelper.RESULT_ERROR -> {
            AutoResolveHelper.getStatusFromIntent(data)
                ?.let {
                  handleError(it.statusCode)
                }
          }
        }

        // Re-enables the Google Pay payment button.
        googlePayButton!!.isClickable = true
      }
    }
  }

  private fun onGooglePayResult(data: Intent) {
    val paymentData = PaymentData.getFromIntent(data) ?: return
    val paymentMethodCreateParams =
      PaymentMethodCreateParams.createFromGooglePay(
          JSONObject(paymentData.toJson())
      )

    // now use the `paymentMethodCreateParams` object to create a PaymentMethod
    stripe!!.createPaymentMethod(
        paymentMethodCreateParams,
        callback = object : ApiResultCallback<PaymentMethod> {
          override fun onSuccess(result: PaymentMethod) {
            handlePaymentSuccess(result)
          }

          override fun onError(e: Exception) {
          }
        }
    )
  }

  /**
   * PaymentData response object contains the payment information, as well as any additional
   * requested information, such as billing and shipping address.
   *
   * @param paymentData A response object returned by Google after a payer approves payment.
   * @see [Payment
   * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
   */
  private fun handlePaymentSuccess(paymentMethod: PaymentMethod) {


    try {
      // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
      val billingName = paymentMethod.billingDetails!!.name
      Log.d("BillingName", billingName)

      Toast.makeText(
          this, getString(R.string.payments_show_name, prePurchasebookTitle), Toast.LENGTH_LONG
      )
          .show()

      paymentSuccess = true
      retrieve()

    } catch (e: JSONException) {
      Log.e("handlePaymentSuccess", "Error: " + e.toString())
    }
  }

  fun retrieve() {

    if (paymentSuccess) {
      userPurchasesMapping?.add(bookId!!, true)
      updateData()
    }


  }

  private fun updateData() {
    val currentUserReference = usersDatabaseReference!!.child("user${firebaseUser!!.uid}")

    currentUserReference!!.setValue(User(firebaseUser!!.uid, userPurchasesMapping))
    val intent = intent
    finish()
    startActivity(intent)

  }

  /**
   * At this stage, the user has already seen a popup informing them an error occurred. Normally,
   * only logging is required.
   *
   * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
   * WalletConstants.ERROR_CODE_* constants.
   * @see [
   * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
   */
  private fun handleError(statusCode: Int) {
    Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode))
  }
  companion object {
    const val TEST_KEY = "sk_test_51IQ8HECuzAs95gvcmI1ixDpPaoWBtrEVlvDJYoCYOuYSmEaucQ6iMytS62fo1p1XKLZncHkaVSeQOT9KUAVNUj7W00XuMwyMEh"
    const val PUBLISHABLE_KEY = "pk_test_51IQ8HECuzAs95gvcBSRsGYl7PgVhVUAwTA3YwBPjvEoqpB9ygaLL97NmxpAzKvxUT89cy7XDZ7iNFf8G5Gz8azFw00hOaPuCBC"

  }
}


