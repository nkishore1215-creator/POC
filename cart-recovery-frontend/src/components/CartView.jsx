import { useState } from 'react'
import deleteIcon from '../assets/delete.jpg'
import { useNavigate } from 'react-router-dom'
import { cartApi } from '../services/api'


const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
})

function CartView({
  items = [],
  cartId,
  customerId,
  onQuantityChange,
  onRemoveItem,
  onCheckout,
  removingCartItemId = null,
  updatingCartItemId = null,
  isCheckingOut = false,
}) {

  const navigate = useNavigate()

  const [discountCode, setDiscountCode] = useState('')
  const [discountPercentage, setDiscountPercentage] = useState(0)
  const [discountMessage, setDiscountMessage] = useState('')

  const subtotal = items.reduce((total, item) => {

    const product = item.product ?? item

    return (
      total +
      (Number(product.price) || 0) *
      (Number(item.quantity) || 0)
    )

  }, 0)

  const discountAmount =
    subtotal * (discountPercentage / 100)

  const finalAmount =
    subtotal - discountAmount


  async function handleApplyDiscount() {

    const code=discountCode.trim()

    if(!code){
      return
    }
     try{
      if(code==='0'){
        setDiscountPercentage(0)
        setDiscountCode('')
        setDiscountMessage("Discount Removed")
      

        const updatedTotal =
        Number(subtotal.toFixed(2))
        await cartApi.update(cartId,{
            customerId: customerId,
            totalAmount:updatedTotal,
            discountAmount:0
          }
        )
        console.log("removed code:" +updatedTotal)
   return
      }


    const result= await cartApi.validateDiscount(
       cartId,
       code
    )

    if(!result.valid){
      setDiscountPercentage(0);
      setDiscountMessage("Invalid Discount Code")
      return

    }

      const percentage= Number(result.discountPercentage) || 0
       const discount =
      subtotal * (percentage / 100)

    const updatedTotal = subtotal - discount;
      setDiscountPercentage(percentage)
      setDiscountMessage( `${percentage}% Discount Applied`)
   
  
   await cartApi.update(cartId, {
        customerId: customerId,
        totalAmount: updatedTotal,
        discountAmount: discount
        
     
      })
      console.log(
      'Applied discount code:', 
      code
    )
console.log(updatedTotal)
  }catch (error) {

      console.error(
        'Failed to update cart',
        error
      )
      console.log(
      'Applied discount code:', 
      code
    )
    
    }

     
  }


  if (items.length === 0) {

    return (
      <section className="cart-view">

        <div className="cart-view__nav">

          <button
            type="button"
            className="products-tab"
            onClick={() => navigate('/')}
          >
            Shop-Products
          </button>

        </div>

        <h2>Your Shopping Cart</h2>

        <p>Your cart is empty</p>

      </section>
    )
  }


  return (

    <section className="cart-view">

      <button
        type="button"
        className="products-tab"
        onClick={() => navigate('/')}
      >
        Shop-Products
      </button>

      <h2>Your Shopping Cart</h2>


      <ul className="cart-view__items">

        {items.map((item) => {

          const product =
            item.product ?? item

          const quantity =
            Number(item.quantity) || 0

          const itemTotal =
            (Number(product.price) || 0) *
            quantity

          const stockQuantity =
            Number(product.stockQuantity) || 0

          const availableStock =
            Math.max(
              stockQuantity - quantity,
              0
            )

          const isInStock =
            availableStock > 0


          const itemId =
            item.cartItemId ??
            item.id ??
            product.productId ??
            product.id


          const productName =
            product.productName ??
            product.name


          const isRemoving =
            removingCartItemId === itemId


          const isUpdating =
            updatingCartItemId === itemId


          return (

            <li
              className="cart-item"
              key={itemId}
            >

              {product.imageUrl ? (

                <img
                  className="cart-item__image"
                  src={product.imageUrl}
                  alt={productName}
                />

              ) : (

                <div  className='cart-item__noImage'>
                  No Image
                </div>

              )}


              <div className="cart-item__details">

                <h3>
                  {productName}
                </h3>

                <p>
                  Price:{' '}
                  {currencyFormatter.format(
                    Number(product.price) || 0
                  )}
                </p>


                <div
                  className="cart-item__quantity"
                  aria-label="Quantity"
                >

                  <button
                    type="button"
                    disabled={
                      !onQuantityChange ||
                      isUpdating ||
                      isRemoving
                    }
                    onClick={() =>
                      onQuantityChange(
                        item,
                        quantity - 1
                      )
                    }
                  >
                    -
                  </button>


                  <span>
                    {quantity}
                  </span>


                  <button
                    type="button"
                    disabled={
                      !onQuantityChange ||
                      isUpdating ||
                      isRemoving ||
                      quantity >= stockQuantity
                    }
                    onClick={() =>
                      onQuantityChange(
                        item,
                        quantity + 1
                      )
                    }
                  >
                    +
                  </button>

                </div>


                <p
                  className={`product-card__stock ${
                    isInStock
                      ? ''
                      : 'is-empty'
                  }`}
                >

                  {isInStock
                    ? `${availableStock} in stock`
                    : 'Out of stock'}

                </p>


                <div className="cart-item__actions">

                  {onRemoveItem && (

                    <button
                      type="button"
                      disabled={
                        isRemoving ||
                        isUpdating
                      }
                      onClick={() => {

                        const confirmed =
                          window.confirm(
                            `Are you sure you want to remove "${productName}" from your cart?`
                          )

                        if (confirmed) {
                          onRemoveItem(item)
                        }

                      }}
                    >

                      {isRemoving ? (

                        'Removing...'

                      ) : (

                        <img
                          style={{
                            width: '20px',
                            height: '20px',
                            display: 'block'
                          }}
                          src={deleteIcon}
                          alt="delete"
                        />

                      )}

                    </button>

                  )}

                </div>

              </div>


              <p className="cart-item">

                {currencyFormatter.format(
                  itemTotal
                )}

              </p>

            </li>
          )

        })}

      </ul>


      {/* ONE DISCOUNT SECTION FOR ENTIRE CART */}

      <div className="discount-section">

        <input
          type="text"
          className="discount-input"
          value={discountCode}
          placeholder="Enter discount code"
          onChange={(e) =>
            setDiscountCode(e.target.value)
          }
        />

        <button
          type="button"
          onClick={handleApplyDiscount}
          disabled={
            !discountCode.trim()
          }
        >
          Apply
        </button>


        {discountMessage && 
        (       
          <p
            style={{
              fontSize: 14,
              color: discountMessage.includes("Invalid") ? "red" : "green"
            }}
          >
            {discountMessage}
          </p>

        )}

      </div>


      <footer className="cart-view__summary">

        <h4>Subtotal</h4>

        <p>
          {currencyFormatter.format(
            subtotal
          )}
        </p>


        {discountPercentage > 0 && (

          <p>
            Discount ({discountPercentage}%):
          

            {currencyFormatter.format(
              discountAmount
            )}
          </p>

        )}


        <h3>
          Total:{' '}
          {currencyFormatter.format(
            finalAmount
          )}
        </h3>


        {onCheckout && (

          <button
            className="cart-view__checkout"
            type="button"
            disabled={isCheckingOut}
            onClick={onCheckout}
          >

            {isCheckingOut
              ? 'Processing...'
              : 'Checkout'}

          </button>

        )}

      </footer>

    </section>
  )
}

export default CartView