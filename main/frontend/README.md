# BT Bank Frontend

A modern, responsive banking application frontend built with React, TypeScript, and TailwindCSS. This application provides a comprehensive interface for the BT Bank microservices backend, supporting customer management, product pricing, fixed deposit calculations, and account management.

## 🚀 Features

### Authentication & Authorization
- **Secure Login/Registration**: JWT-based authentication with token management
- **Role-based Access Control**: Support for Customer, Bank Officer, and Admin roles
- **Protected Routes**: Automatic redirection and access control
- **Token Refresh**: Seamless token management and refresh

### Banking Features
- **Dashboard**: Comprehensive overview with statistics and quick actions
- **Customer Profile**: Personal information management and security settings
- **Product Management**: View, create, and edit banking products (role-based)
- **FD Calculator**: Interactive fixed deposit return calculator
- **Account Management**: View account details, transactions, and statements
- **Admin Controls**: Full administrative capabilities for bank officers

### User Experience
- **Responsive Design**: Mobile-first approach with seamless desktop experience
- **Modern UI**: Built with shadcn/ui components and TailwindCSS
- **Dark/Light Mode**: Theme switching with system preference detection
- **Loading States**: Comprehensive loading indicators and skeleton screens
- **Error Handling**: Error boundaries and user-friendly error messages
- **Form Validation**: Real-time validation with Zod schemas
- **Accessibility**: WCAG compliant with keyboard navigation support

## 🛠️ Technology Stack

- **Framework**: React 19 with TypeScript
- **Build Tool**: Vite
- **Styling**: TailwindCSS v4 with custom design system
- **UI Components**: shadcn/ui with Radix UI primitives
- **Routing**: React Router v7
- **State Management**: React Context API
- **Form Handling**: React Hook Form with Zod validation
- **HTTP Client**: Axios with interceptors
- **Icons**: Lucide React
- **Notifications**: Sonner toast notifications
- **Package Manager**: pnpm

## 📁 Project Structure

```
src/
├── components/           # Reusable UI components
│   ├── auth/            # Authentication components
│   ├── layout/          # Layout components (sidebar, navigation)
│   └── ui/              # shadcn/ui components
├── context/             # React Context providers
├── hooks/               # Custom React hooks
├── lib/                 # Utility functions and configurations
├── pages/               # Page components
│   ├── auth/            # Login, Register pages
│   ├── accounts/        # Account management pages
│   ├── customer/        # Customer profile pages
│   ├── fd/              # FD calculator pages
│   └── products/        # Product management pages
└── assets/              # Static assets
```

## 🚀 Getting Started

### Prerequisites
- Node.js 18+ 
- pnpm (recommended) or npm
- Backend API running on port 8080

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd bt-lab-project-master/main/frontend
   ```

2. **Install dependencies**
   ```bash
   pnpm install
   ```

3. **Start development server**
   ```bash
   pnpm dev
   ```

4. **Open in browser**
   Navigate to `http://localhost:5173`

### Available Scripts

- `pnpm dev` - Start development server
- `pnpm build` - Build for production
- `pnpm preview` - Preview production build
- `pnpm lint` - Run ESLint

## 🔧 Configuration

### Environment Variables
Create a `.env.local` file in the root directory:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=BT Bank
```

### API Configuration
The application is configured to connect to the backend API gateway on port 8080. Update the base URL in `src/lib/api.ts` if your backend runs on a different port.

## 🎨 Design System

### Color Palette
- **Primary**: Orange-based brand colors
- **Secondary**: Neutral grays and blues
- **Success**: Green for positive actions
- **Warning**: Yellow for caution
- **Error**: Red for errors and destructive actions

### Typography
- **Font Family**: Inter (sans-serif)
- **Font Sizes**: Responsive scale from 12px to 48px
- **Font Weights**: 400 (normal), 500 (medium), 600 (semibold), 700 (bold)

### Spacing
- **Base Unit**: 4px (0.25rem)
- **Scale**: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96px

## 🔐 Authentication Flow

1. **Login/Register**: Users authenticate via email/password
2. **Token Storage**: JWT tokens stored in localStorage
3. **API Requests**: Automatic token attachment to requests
4. **Token Refresh**: Automatic refresh on 401 responses
5. **Logout**: Token cleanup and redirect to login

## 👥 User Roles & Permissions

### Customer
- View personal profile and account details
- Use FD calculator
- View own accounts and transactions
- Access product information

### Bank Officer
- All customer permissions
- Create and edit banking products
- View all customer accounts
- Access administrative functions

### Admin
- All bank officer permissions
- Full system access
- User management capabilities
- System configuration

## 📱 Responsive Design

### Breakpoints
- **Mobile**: < 640px
- **Tablet**: 640px - 1024px
- **Desktop**: > 1024px

### Mobile Features
- Collapsible sidebar navigation
- Touch-friendly interface
- Optimized form layouts
- Swipe gestures support

## 🧪 Testing

### Component Testing
```bash
# Run component tests
pnpm test

# Run tests in watch mode
pnpm test:watch

# Generate coverage report
pnpm test:coverage
```

### E2E Testing
```bash
# Run end-to-end tests
pnpm test:e2e
```

## 🚀 Deployment

### Production Build
```bash
pnpm build
```

### Deployment Options
- **Static Hosting**: Vercel, Netlify, GitHub Pages
- **CDN**: CloudFront, Cloudflare
- **Container**: Docker with Nginx

### Environment Configuration
Update the API base URL for production:
```typescript
// src/lib/api.ts
export const api = axios.create({
  baseURL: process.env.VITE_API_BASE_URL || 'https://api.btbank.com',
  // ...
})
```

## 🔧 Development Guidelines

### Code Style
- Use TypeScript for type safety
- Follow React best practices
- Use functional components with hooks
- Implement proper error handling
- Write self-documenting code

### Component Guidelines
- Keep components small and focused
- Use proper prop types
- Implement loading and error states
- Follow accessibility guidelines
- Use semantic HTML

### State Management
- Use React Context for global state
- Local state for component-specific data
- Custom hooks for reusable logic
- Proper cleanup in useEffect

## 🐛 Troubleshooting

### Common Issues

1. **API Connection Failed**
   - Check if backend is running on port 8080
   - Verify CORS configuration
   - Check network connectivity

2. **Authentication Issues**
   - Clear localStorage and try again
   - Check token expiration
   - Verify JWT secret configuration

3. **Build Errors**
   - Clear node_modules and reinstall
   - Check TypeScript configuration
   - Verify all dependencies are installed

### Debug Mode
Enable debug logging by setting:
```typescript
localStorage.setItem('debug', 'true')
```

## 📚 API Integration

### Endpoints Used
- `POST /auth/login` - User authentication
- `POST /auth/register` - User registration
- `GET /dashboard/stats` - Dashboard statistics
- `GET /customer/profile` - Customer profile
- `PUT /customer/profile` - Update profile
- `GET /products` - List products
- `POST /products` - Create product
- `PUT /products/:id` - Update product
- `DELETE /products/:id` - Delete product
- `POST /fd-calculator/calculate` - Calculate FD returns
- `GET /accounts` - List accounts
- `GET /accounts/:id` - Account details
- `GET /accounts/:id/transactions` - Account transactions

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

### Commit Convention
Use conventional commits:
- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation changes
- `style:` Code style changes
- `refactor:` Code refactoring
- `test:` Test additions/changes

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation wiki

---

**Built with ❤️ for BT Bank Digital Banking Platform**