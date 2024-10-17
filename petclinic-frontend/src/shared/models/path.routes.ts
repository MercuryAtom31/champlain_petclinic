export enum AppRoutePaths {
  Default = '/',
  MoveInventoryProducts = 'inventory/:inventoryId/products/:productId/move',
  EditInventory = 'inventories/inventory/:inventoryId/edit',
  EditInventoryProducts = 'inventory/:inventoryId/products/:productId/edit',
  LowStockProducts = '/products/lowstock',
  AddSupplyToInventory = 'inventory/:inventoryId/products/add',
  CustomerEmergency = '/customer/emergency',
  Review = '/reviews',
  EmergencyById = '/visits/emergency/:visitEmergencyId',
  Emergency = '/add/emergency',
  EmergencyList = '/visits/emergencyList',
  //EditEmergency = '/visits/emergency/:emergencyId',
  UpdateReview = '/updateReview/:reviewId/edit',
  GetVisitByVistId = 'visits/:visitId',
  Form = '/forms',
  Inventories = '/inventories',
  Vet = '/vets',
  InventoryProducts = '/inventory/:inventoryId/products',
  InventorySupplies = '/inventories/:inventoryName/supplies',
  AdminBills = '/bills/admin',
  UpdateBill = '/bills/admin/:billId/edit',
  CustomerBills = '/bills/customer',
  PageNotFound = '/page-not-found',
  Unauthorized = '/unauthorized',
  RequestTimeout = '/request-timeout',
  InternalServerError = '/internal-server-error',
  ServiceUnavailable = '/service-unavailable',
  Login = '/users/login',
  CustomerProfileEdit = '/customer/profile/edit',
  AddingCustomer = '/customer/add',
  AllCustomers = '/customers',
  Home = '/home',
  Forbidden = '/forbidden',
  Products = '/products',
  Visits = '/visits',
  Carts = '/carts',
  UserCart = '/carts/:cartId',
  AddVisit = '/visits/add',
  CustomerProfile = '/customer/profile',
  Emailing = '/emailing',
  MockPage = '/mockpage',
  VetDetails = '/vets/{vetId}',
  EditVisit = '/visits/:visitId/edit',
  CustomerDetails = '/customers/:ownerId',
  AddPet = '/customers/:ownerId/pets/new',
  UpdateCustomer = '/customers/:ownerId/edit',
  Promos = '/promos',
  AddPromo = '/promos/add',
  UpdatePromo = 'promos/:promoId/edit',
  SignUp = '/users/signup',
  CustomerVisits = '/customer/visits',
  UpdatePet = '/pets/:petId/edit',
  ProductDetails = '/products/:productId',
  EditProduct = '/products/edit/:productId',
  ForgotPassword = '/users/forgot-password',
  ResetPassword = '/users/reset-password/:token',
  CustomerReviews = '/customers/visits/reviews',
  CustomerAddReview = '/customers/visits/reviews/add',
  AllUsers = '/users',
}
