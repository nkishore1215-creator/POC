import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { authApi } from "../services/api"
import "../services/Login.css"


function Login() {

  const navigate = useNavigate()

  const [login, setLogin] = useState({
    email: '',
    password: ''
  })

  const [error, setError] = useState('')

  function handleChange(e) {
    const { name, value } = e.target

    setLogin((current) => ({
      ...current,
      [name]: value
    }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError("")

    try {

      const customer = await authApi.login({
        email: login.email,
        password: login.password
      })

      localStorage.setItem("loggedIn", "true")
      localStorage.setItem("customerId", customer.id)
      localStorage.setItem("username", customer.firstname)
      localStorage.setItem("lastname", customer.lastname)
      localStorage.setItem("email", customer.email)

      console.log(
        "customerId from storage:",
        localStorage.getItem("customerId")
      )

      navigate('/')

    } catch (error) {
      console.error("Login error", error)
      setError("Invalid credentials. Please check your email and password.")
    }
  }

  return (
    <main className="login-page">

      <div className="login-card">

        <div className="login-header">
          <div className="login-logo">🛒</div>

          <h1>Welcome Back</h1>

          <p>
            Sign in to continue to Cart Recovery Store
          </p>
        </div>

        {error && (
          <p className="error-message">
            {error}
          </p>
        )}

        <form onSubmit={handleSubmit}>

          <div className="login-form-group">
            <label htmlFor="email">
              Email:
            </label>

            <input
              id="email"
              type="email"
              name="email"
              value={login.email}
              onChange={handleChange}
              placeholder="Enter your email"
              required
            />
          </div>

          <div className="login-form-group">
            <label htmlFor="password">
              Password:
            </label>

            <input
              id="password"
              type="password"
              name="password"
              value={login.password}
              onChange={handleChange}
              placeholder="Enter your password"
              required
            />
          </div>
      <div className="login-buttons">
         <button
            type="submit"
            className="login-button"
          >
            Sign In
          </button>
          <button 
            type="button"            
            className="login-button"
             onClick={()=> navigate('/signup')}
          >
            Sign Up
          </button>
</div>
        </form>

        

      </div>

    </main>
  )
}

export default Login