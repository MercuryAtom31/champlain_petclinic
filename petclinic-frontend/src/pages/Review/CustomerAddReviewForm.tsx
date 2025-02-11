import AddCustomerReviewForm from '@/features/visits/Review/AddReviewCustomerForm';
import { NavBar } from '@/layouts/AppNavBar';

export default function AddReviewsCustomer(): JSX.Element {
  return (
    <div>
      <NavBar />
      <h1>Leave a review</h1>
      <AddCustomerReviewForm />
    </div>
  );
}
