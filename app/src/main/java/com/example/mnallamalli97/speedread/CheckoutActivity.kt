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
import com.bumptech.glide.Glide
import com.example.mnallamalli97.speedread.R.layout
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToLong

class CheckoutActivity : AppCompatActivity() {
  private var prePurchasebookTitle: TextView? = null
  private var prePurchaseBookAuthor: TextView? = null
  private var prePurchaseBookCover: ImageView? = null
  private var readSummaryButton: Button? = null
  private var readNowButton: Button? = null
  private var databaseReference: DatabaseReference? = null
  private var googlePayButton: RelativeLayout? = null
  private var isDarkModeOn = false
  private var bookPrice: Float? = null
  private var pref: SharedPreferences? = null
  private var editor: SharedPreferences.Editor? = null
  private lateinit var paymentsClient: PaymentsClient
  private var isBookPurchased: Boolean = false
  private var paymentSuccess: Boolean = false
  private var bookId: Int? = null
  private val LOAD_PAYMENT_DATA_REQUEST_CODE = 1214

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
    val bookSummaryPath = extras.getString("book_summary_path")
    val author = extras.getString("author")
    val title = extras.getString("title")
    val bookCoverPath = extras.getString("cover")
    bookPrice = extras.getFloat("book_price")


    retrieve()
    // Initialize a Google Pay API client for an environment suitable for testing.
    // It's recommended to create the PaymentsClient object inside of the onCreate method.
    paymentsClient = PaymentsUtil.createPaymentsClient(this)
    possiblyShowGooglePayButton()

    googlePayButton!!.setOnClickListener { requestPayment() }

    prePurchaseBookAuthor!!.text = author
    prePurchasebookTitle!!.text = title
    val url = bookCoverPath
    Glide.with(applicationContext)
        .load(url)
        .error(R.drawable.ic_library_books_24px)
        .into(prePurchaseBookCover!!)
    prePurchaseBookCover!!.isEnabled = false

    readSummaryButton!!.setOnClickListener {
      val intent = Intent(this@CheckoutActivity, SettingsActivity::class.java)
      intent.putExtra("title", title)
      intent.putExtra("book_summary_path", bookSummaryPath)
      intent.putExtra("book_path", bookPath)
      intent.putExtra("id", bookId!!)
      intent.putExtra("author", author)
      intent.putExtra("cover", bookCoverPath)

      startActivity(intent)
    }

    readNowButton!!.setOnClickListener {
      val intent = Intent(this@CheckoutActivity, SettingsActivity::class.java)
      intent.putExtra("title", title)
      intent.putExtra("book_path", bookPath)
      intent.putExtra("book_summary_path", bookSummaryPath)
      intent.putExtra("id", bookId!!)
      intent.putExtra("author", author)
      intent.putExtra("cover", bookCoverPath)

      startActivity(intent)
    }
  }

  private fun possiblyShowGooglePayButton() {
    val isReadyToPayJson = PaymentsUtil.isReadyToPayRequest() ?: return
    val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString()) ?: return

    // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
    // OnCompleteListener to be triggered when the result of the call is known.
    val task = paymentsClient.isReadyToPay(request)
    task.addOnCompleteListener { completedTask ->
      try {
        completedTask.getResult(ApiException::class.java)
            ?.let(::setGooglePayAvailable)
      } catch (exception: ApiException) {
        // Process error
        Log.w("isReadyToPay failed", exception)
      }
    }
  }

  /**
   * If isReadyToPay returned `true`, show the button and hide the "checking" text. Otherwise,
   * notify the user that Google Pay is not available. Please adjust to fit in with your current
   * user flow. You are not required to explicitly let the user know if isReadyToPay returns `false`.
   *
   * @param available isReadyToPay API response.
   */
  private fun setGooglePayAvailable(available: Boolean) {
    if (available) {
      googlePayButton!!.visibility = View.VISIBLE
    } else {
      Toast.makeText(
          this,
          "Unfortunately, Google Pay is not available on this device",
          Toast.LENGTH_LONG
      )
          .show();
    }
  }

  private fun requestPayment() {

    // Disables the button to prevent multiple clicks.
    googlePayButton!!.isClickable = false

    // The price provided to the API should include taxes and shipping.
    val priceCents = bookPrice!!.times(PaymentsUtil.CENTS.toLong())
        .roundToLong()

    val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(priceCents)
    if (paymentDataRequestJson == null) {
      Log.e("RequestPayment", "Can't fetch payment data request")
      return
    }
    val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

    // Since loadPaymentData may show the UI asking the user to select a payment method, we use
    // AutoResolveHelper to wait for the user interacting with it. Once completed,
    // onActivityResult will be called with the result.
    if (request != null) {
      AutoResolveHelper.resolveTask(
          paymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE
      )
    }
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
            data?.let { intent ->
              // removing the Buy button and replacing with "Read Now button"
              googlePayButton!!.visibility = View.GONE
              readNowButton!!.visibility = View.VISIBLE

              PaymentData.getFromIntent(intent)
                  ?.let(::handlePaymentSuccess)
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

  /**
   * PaymentData response object contains the payment information, as well as any additional
   * requested information, such as billing and shipping address.
   *
   * @param paymentData A response object returned by Google after a payer approves payment.
   * @see [Payment
   * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
   */
  private fun handlePaymentSuccess(paymentData: PaymentData) {
    val paymentInformation = paymentData.toJson() ?: return

    try {
      // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
      val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")
      val billingName = paymentMethodData.getJSONObject("info")
          .getJSONObject("billingAddress")
          .getString("name")
      Log.d("BillingName", billingName)

      Toast.makeText(
          this, getString(R.string.payments_show_name, prePurchasebookTitle), Toast.LENGTH_LONG
      )
          .show()

      // Logging token string.
      Log.d(
          "GooglePaymentToken", paymentMethodData
          .getJSONObject("tokenizationData")
          .getString("token")
      )

      paymentSuccess = true
      retrieve()

    } catch (e: JSONException) {
      Log.e("handlePaymentSuccess", "Error: " + e.toString())
    }
  }

  fun retrieve() {
    databaseReference = FirebaseDatabase.getInstance()
        .getReference("speedread")
        .child("books")

    databaseReference!!.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        for (ds in dataSnapshot.children) {
          val book = ds.getValue(Book::class.java)

          if (paymentSuccess) {
            updateData(book)
          }

          if (bookId == book!!.id) {
            if (book!!.purchased!!) {
              googlePayButton!!.visibility = View.GONE
              readNowButton!!.visibility = View.VISIBLE
            } else {
              googlePayButton!!.visibility = View.VISIBLE
              readNowButton!!.visibility = View.GONE
            }
          }
        }
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Log.d(ContentValues.TAG, databaseError.message) // Don't ignore errors!
      }
    })
  }

  private fun updateData(book: Book?) {
    if (bookId == book!!.id) {
      databaseReference!!.child("book$bookId")
          .child("purchased")
          .setValue(true)
      val intent = intent
      finish()
      startActivity(intent)
    }
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
}


