const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
})

function ProductCard({
  product,
  quantityInCart = 0,
  onAddToCart,
  isAdding = false,
}) {
  const name = product.productName ?? product.name
  const { description, imageUrl, price } = product
  const stockQuantity = Number(product.stockQuantity ?? product.quantity ?? 0)
  const availableStock = Math.max(stockQuantity - quantityInCart, 0)
  const isInStock = availableStock > 0

  return (
    <article className="product-card">
      {imageUrl && (
        <img className="product-card__image" src={product.imageUrl} alt={name} />
      )}

      <div className="product-card__content">
        <h3>{name}</h3>
        {description && <p>{description}</p>}
        <p className="product-card__price">
          {currencyFormatter.format(Number(price) || 0)}
        </p>

        <p className={`product-card__stock ${isInStock ? '' : 'is-empty'}`}>
          {isInStock ? `${availableStock} in stock` : 'Out of stock'}
        </p>

        <button
          type="button"
          disabled={!isInStock || !onAddToCart || isAdding}
          onClick={() => onAddToCart(product)}
          
        >
          {isAdding ? 'Adding...' : isInStock ? 'Add to cart' : 'Out of stock'}
        </button>
        
      </div>
    </article>
  )
}

export default ProductCard
