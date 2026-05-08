package io.github.dot166.nexus

import android.os.Bundle
import androidx.activity.ComponentActivity

class DefaultStub: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stub)
    }
}