import { useEffect, useState } from 'react'
import CartView from './components/CartView.jsx'
import ProductList from './components/ProductList.jsx'
import { cartApi, productApi } from './services/api.js'
import shoppingCart from "./assets/shoppingCart.png"
import './App.css'
import {Routes,Route,useNavigate, useLocation} from 'react-router-dom'
import RcsPreview from './components/RcsPreview.jsx'
import AddProduct from './components/AddProduct.jsx'
import Login from './components/Login.jsx'
import SignUp from './components/SignUp.jsx'


//const CUSTOMER_ID = Number(import.meta.env.VITE_CUSTOMER_ID ?? 1)


const EDITABLE_CART_STATUSES = new Set(['ACTIVE', 'ABANDONED', 'REMINDER_SENT'])

function App() {
  const navigate = useNavigate()
  const CUSTOMER_ID= Number(localStorage.getItem("customerId"))
  const firstName=localStorage.getItem("username")
  const lastName=localStorage.getItem("lastname")
  const handleLogout= ()=>{
    localStorage.removeItem("customerId")
    localStorage.removeItem("firstName")
    localStorage.removeItem("lastName")
    
    navigate("/login",{replace:true})
  }
  const [products, setProducts] = useState([])
  const [cartItems, setCartItems] = useState([])
  const [cartId, setCartId] = useState(null)
  const [addingProductId, setAddingProductId] = useState(null)
  const [removingCartItemId, setRemovingCartItemId] = useState(null)
  const [updatingCartItemId, setUpdatingCartItemId] = useState(null)
  const [isCheckingOut, setIsCheckingOut] = useState(false)
  const [loading, setLoading] = useState(true)
  const [productError, setProductError] = useState('')
  const [actionError, setActionError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('ALL')
  const [currentpage, setCurrentPage] = useState(1)
  const productsPerPage = 6
  const [searchText, setSearchText]= useState('')
  const [searchTerm, setSearchTerm]= useState('')
  const [showAddProduct, setShowAddProduct]= useState(false)
  const [newProduct,setNewproduct]=useState({
    productName:'',
    description:'',
    price:'',
    stockQuantity:'',
    category:'MOBILE'
  }

  )

  useEffect(() => {

    if(location.pathname==="/login" || location.pathname==="/signup"){
      setLoading(false)
      return
    }
    const customerId=localStorage.getItem("customerId")
    if(!customerId){
      navigate("/login",{replace:true})
      return
    }

    let active = true

    Promise.all([productApi.getAll(), cartApi.getByCustomer(CUSTOMER_ID)])
      .then(async ([productData, carts]) => {
        const editableCart = [...carts]
          .sort(
            (left, right) =>
              new Date(right.createdAt).getTime() -
              new Date(left.createdAt).getTime(),
          )
          .find((cart) => EDITABLE_CART_STATUSES.has(cart.cartStatus))
        const items = editableCart
          ? await cartApi.getItems(editableCart.cartId)
          : []

        if (active) {
          setProducts(productData)
          setCartId(editableCart?.cartId ?? null)
          setCartItems(items)
        }
      })
      .catch((requestError) => {
        if (active) setProductError(requestError.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [location.pathname])

  useEffect(() => {
    if (!successMessage) return undefined
    const timeoutId = window.setTimeout(() => setSuccessMessage(''), 1000)
    return () => window.clearTimeout(timeoutId)
  }, [successMessage])

  async function handleAddToCart(product) {
    const productId = product.productId ?? product.id
    const productName = product.productName ?? product.name

    setAddingProductId(productId)
    setActionError('')

    try {
      let targetCartId = cartId
      if (!targetCartId) {
        const newCart = await cartApi.create({
          customerId: CUSTOMER_ID,
          totalAmount: 0,
        })
        targetCartId = newCart.cartId
        setCartId(targetCartId)
      }

      await cartApi.addItem(targetCartId, { productId, quantity: 1 })
      setCartItems(await cartApi.getItems(targetCartId))
      setSuccessMessage(`${productName} has been added to your cart.`)
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setAddingProductId(null)
    }
  }

  async function handleRemoveFromCart(item) {
    if (!cartId) return

    const cartItemId = item.cartItemId ?? item.id
    const productName =
      item.product?.productName ?? item.product?.name ?? 'Product'

    setRemovingCartItemId(cartItemId)
    setActionError('')

    try {
      await cartApi.removeItem(cartId, cartItemId)
      setCartItems(await cartApi.getItems(cartId))
      setSuccessMessage(`${productName} was removed from your cart.`)
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setRemovingCartItemId(null)
    }
  }

  async function handleQuantityChange(item, quantity) {
    if (!cartId) return

    if (quantity <= 0) {
      await handleRemoveFromCart(item)
      return
    }

    const cartItemId = item.cartItemId ?? item.id
    const productId = item.product?.productId ?? item.product?.id

    setUpdatingCartItemId(cartItemId)
    setActionError('')

    try {
      await cartApi.updateItem(cartId, cartItemId, { productId, quantity })
      setCartItems((currentItems) =>
        currentItems.map((currentItem) =>
          (currentItem.cartItemId ?? currentItem.id) === cartItemId
            ? { ...currentItem, quantity }
            : currentItem,
        ),
      )
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setUpdatingCartItemId(null)
    }
  }

  async function handleCheckout() {
    if (!cartId || cartItems.length === 0) return

    setIsCheckingOut(true)
    setActionError('')

    try {
      await cartApi.checkout(cartId)
      setProducts(await productApi.getAll())
      setCartItems([])
      setCartId(null)
      setSuccessMessage('Checkout completed successfully.')
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setIsCheckingOut(false)
    }
  }

  
  const cartCount = cartItems.reduce(
    (total, item) => total + (Number(item.quantity) || 0),
    0,
  )


const filteredProducts=products.filter((prodcut)=>
{
  const matchesCategory= selectedCategory==='ALL' || prodcut.category===selectedCategory

 // const matcheSearch= !searchTerm || prodcut.productName?.toLowerCase().trim()===searchTerm.toLowerCase().trim()
  const matchSearch= !searchTerm || prodcut.productName?.toLowerCase().includes(searchTerm.toLowerCase().trim())

  return matchesCategory && matchSearch
}
)

  // const filteredProducts=products.filter((product) => {
  //   if (selectedCategory === 'ALL') return true
  //   return product.category === selectedCategory
  // })

  const lastProducetPage= currentpage*productsPerPage
  const firstProdcutPage= lastProducetPage-productsPerPage

  const currentProducts= filteredProducts.slice(firstProdcutPage,lastProducetPage)

  const totalpages= Math.ceil(filteredProducts.length/productsPerPage)

  return (
    <Routes>
      <Route
      path='/login'
      element={<Login/>}
      />
      <Route
      path='/signup'
      element={<SignUp/>}
      />
      <Route
        path="/"
        element={
          <main className="storefront">
            <header className="storefront__header">
              <div className="storefront__topbar">               
                <h1 className="storefront__eyebrow">Cart Recovery Store</h1>
                         <div className="user-info">
    Welcome, 
    <span>{firstName} {lastName}</span>
    <button type='button' style={{width:60, height:28, borderRadius:8, cursor:'pointer', backgroundColor:"yellowgreen",marginLeft:20}}
    onClick={handleLogout}> LogOut</button>
  </div>
              <div >
              <button type="button" className='addToProduct'
                   onClick={()=> navigate('/add-product')}
                > Add Product</button>
     </div>
        
                {products.length>0 &&
                <button
                  type="button"
                  className="addToCartButton"
               onClick={() => navigate(`/cart/${cartId}`)}
                >
                  <span className="storefront__cart" aria-live="polite">
                    <img
                      src={shoppingCart}
                      alt="shoppingCart"
                      className="shoppingCart"
                    />
                    <span>Cart: {cartCount}</span>
                  </span>
                </button>
        }
              </div>
              <h2>Shop products</h2>
              <p>Browse the products currently available in our catalog.</p>
             <div className='product-search'>
              <input
              type='text'
              placeholder='Search Prodcut'
              value={searchText}
              onChange={(e)=> setSearchText(e.target.value)}
              ></input>
      <button
      type='button'
      onClick={()=> {setSearchTerm(searchText.trim())
                   setCurrentPage(1) }   
      }
      > Search</button>
              
             </div>

           
            </header>
      

            {successMessage && (
              <p className="storefront__success" role="status">
                {successMessage}
              </p>
            )}
            {actionError && <p role="alert">{actionError}</p>}
              <div className="product-tabs">
              <button
            className={`product-tabs ${selectedCategory === 'ALL' ? 'active' : ''}`}
            onClick={() => {setSelectedCategory('ALL')
              setSearchText('')
              setSearchTerm('')
              setCurrentPage(1)}}
          >
            All Products                              
              </button>
            <button
            className={`product-tabs ${selectedCategory === 'MOBILE' ? 'active' : ''}`}
            onClick={() => {setSelectedCategory('MOBILE')
              setSearchText('')
              setSearchTerm('')
              setCurrentPage(1)
            }}
          >
            Mobiles                              
              </button>
              <button
    className={`product-tabs ${selectedCategory === 'LAPTOP' ? 'active' : ''}`}
    onClick={() => {setSelectedCategory('LAPTOP')
       setSearchText('')
         setSearchTerm('')
      setCurrentPage(1)}}
  >
    Laptops
  </button>
     <button
     className={`product-tabs ${selectedCategory === 'TV' ? 'active' : ''}`}
     onClick={() => {setSelectedCategory('TV')
         setSearchText('')
         setSearchTerm('')
      setCurrentPage(1)}}
   >
     TV
   </button>
                </div>   

               {filteredProducts.length=== 0 && !loading ?
              (<p> No products found  "{searchTerm}"</p>

              ) :(
              

            <ProductList
              products={currentProducts}
              cartItems={cartItems}
              loading={loading}
              error={productError}
              onAddToCart={handleAddToCart}
              addingProductId={addingProductId}
            />)}
            {filteredProducts.length>0 &&
           <div
           className="pagination">
            <button type="button"
            disabled={currentpage===1}
            onClick={()=>setCurrentPage((page)=>Math.max(page-1,1))}
              > Previous
            </button>
           <span className='page-info'>            
    Page {currentpage} of {totalpages || 1}
  </span>
            <button
            type="button" disabled={currentpage>=totalpages || totalpages==0}
            onClick={()=>setCurrentPage((page)=>page+1)} 
            >
              Next
            </button>
           </div>
        }

          </main>
        }
      />



      <Route
        path="/cart/:cartId"
        element={
          <CartView
          cartId={cartId}
          customerId={CUSTOMER_ID}
            items={cartItems}
            onQuantityChange={handleQuantityChange}
            onRemoveItem={handleRemoveFromCart}
            removingCartItemId={removingCartItemId}
            updatingCartItemId={updatingCartItemId}
            onCheckout={handleCheckout}
            isCheckingOut={isCheckingOut}
          />
        }
      />
      <Route 
      path="/add-product"
      element={<AddProduct/>}
      />
      <Route
      
    path="/rcs-preview/:cartId"
    element={<RcsPreview />}
  />
    </Routes>

    
  )
 
}


export default App
