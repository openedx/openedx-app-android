package org.openedx.auth.presentation.lmsselection

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.openedx.auth.R

/**
 * Full-screen QR scanner for the LMS Directory "Sign in with QR code" flow.
 *
 * Wraps zxing's [DecoratedBarcodeView] — which draws the framing viewfinder (the scan
 * rectangle) — and adds a Close button so the learner can back out of the full-screen
 * camera without relying on the system Back gesture. The scanned contents are returned
 * to the caller through ScanContract, exactly like the default zxing CaptureActivity.
 */
class LmsQrScannerActivity : Activity() {

    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_qr_scanner)
        barcodeScannerView = findViewById(R.id.barcode_scanner)
        capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()
        findViewById<View>(R.id.qr_close).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward to CaptureManager so the camera starts once permission is granted.
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
