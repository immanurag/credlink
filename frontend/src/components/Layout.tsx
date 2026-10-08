import React, { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import VoiceFAB from './VoiceFAB'
import { ToastProvider } from '../context/ToastContext'
import AddCustomerModal from './AddCustomerModal'
import AddTransactionModal from './AddTransactionModal'
import RecordPaymentModal from './RecordPaymentModal'

export default function Layout() {
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const [isAddCustomerOpen, setIsAddCustomerOpen] = useState(false)
  const [isTransactionOpen, setIsTransactionOpen] = useState(false)
  const [isPaymentOpen, setIsPaymentOpen] = useState(false)

  // Listen to custom global events or location changes if needed
  const handleSuccess = () => {
    // Dispatch custom event to trigger page refresh in children if needed
    window.dispatchEvent(new CustomEvent('credlink:data-updated'))
  }

  return (
    <ToastProvider>
      <div className="flex min-h-screen bg-slate-50 text-slate-800 antialiased">
        <Sidebar
          isOpen={mobileSidebarOpen}
          onClose={() => setMobileSidebarOpen(false)}
        />

        <div className="flex-1 flex flex-col min-w-0">
          <Topbar
            onToggleSidebar={() => setMobileSidebarOpen(!mobileSidebarOpen)}
            onOpenAddCustomer={() => setIsAddCustomerOpen(true)}
            onOpenTransaction={() => setIsTransactionOpen(true)}
            onOpenPayment={() => setIsPaymentOpen(true)}
          />

          <main className="flex-1 p-4 md:p-6 lg:p-8 max-w-7xl w-full mx-auto space-y-6">
            <Outlet context={{
              openAddCustomer: () => setIsAddCustomerOpen(true),
              openTransaction: () => setIsTransactionOpen(true),
              openPayment: () => setIsPaymentOpen(true)
            }} />
          </main>
        </div>

        <VoiceFAB />

        {/* Global Modals accessible anywhere in the app */}
        <AddCustomerModal
          isOpen={isAddCustomerOpen}
          onClose={() => setIsAddCustomerOpen(false)}
          onSuccess={handleSuccess}
        />

        <AddTransactionModal
          isOpen={isTransactionOpen}
          onClose={() => setIsTransactionOpen(false)}
          onSuccess={handleSuccess}
        />

        <RecordPaymentModal
          isOpen={isPaymentOpen}
          onClose={() => setIsPaymentOpen(false)}
          onSuccess={handleSuccess}
        />
      </div>
    </ToastProvider>
  )
}
