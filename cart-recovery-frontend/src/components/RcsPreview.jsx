import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'

function RcsPreview() {

  const { cartId } = useParams()
  const navigate = useNavigate()

  const [notification, setNotification] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {

    fetch(`http://localhost:8080/api/notifications/cart/${cartId}`)
      .then(response => {

        if (!response.ok) {
          throw new Error('Failed to load RCS notification')
        }

        return response.json()
      })
      .then(data => {

        console.log('Notifications:', data)

        const rcsMessages = data
          .filter(
            item =>
              item.notificationChannel === 'RCS' &&
              item.notificationStatus === 'SENT' &&
              item.message
          )
          .sort(
            (a, b) =>
              new Date(b.sentAt) - new Date(a.sentAt)
          )

        setNotification(rcsMessages[0] ?? null)
      })
      .catch(error => {
        console.error('RCS error:', error)
      })
      .finally(() => {
        setLoading(false)
      })

  }, [cartId])

  if (loading) {
    return <p>Loading RCS message...</p>
  }

  if (!notification) {
    return <p>No RCS message found for cart {cartId}.</p>
  }

  return (
    <div className="rcs-page">

      <div className="rcs-card">

        <h3>Cart Recovery Store</h3>

        <small>✓ Business message</small>

        <p className="rcs-message">
          {notification.message}
        </p>

        <button
          onClick={() => navigate(`/cart/${cartId}`)}
        >
          View cart
        </button>

        <button
          onClick={() => navigate('/')}
        >
          Shop now
        </button>

      </div>

    </div>
  )
}

export default RcsPreview