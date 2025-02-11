import axiosInstance from '@/shared/api/axiosInstance.ts';
import { VisitResponseModel } from '../models/VisitResponseModel';

export async function getAllOwnerVisits(
  ownerId: string
): Promise<VisitResponseModel[]> {
  try {
    const response = await axiosInstance.get(`visits/owners/${ownerId}`);

    if (response.status !== 200) {
      throw new Error(`Error: ${response.status} ${response.statusText}`);
    }
    return response.data
      .split('data:')
      .map((dataChunk: string) => {
        try {
          if (dataChunk == '') return null;
          return JSON.parse(dataChunk);
        } catch (err) {
          console.error('Could not parse JSON: ' + err);
        }
      })
      .filter((data?: JSON) => data !== null);
  } catch (error) {
    throw error;
  }
}
