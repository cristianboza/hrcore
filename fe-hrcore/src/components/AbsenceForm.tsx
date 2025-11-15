import {useState} from 'react';
import {useSubmitAbsenceRequest} from '../hooks/useAbsence';
import {useAuthStore} from '../store/authStore';
import {ABSENCE_TYPE, AbsenceType} from '../types/absence';

interface AbsenceFormProps {
    onSuccess?: () => void;
}

export const AbsenceForm: React.FC<AbsenceFormProps> = ({onSuccess}) => {
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [type, setType] = useState<AbsenceType>(ABSENCE_TYPE.VACATION);
    const [reason, setReason] = useState('');
    const currentUser = useAuthStore((state) => state.currentUser);
    const {mutate: submitRequest, isPending} = useSubmitAbsenceRequest();

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!startDate || !endDate || !currentUser) return;

        submitRequest(
            {
                userId: currentUser.id,
                startDate,
                endDate,
                type,
                reason,
            },
            {
                onSuccess: () => {
                    setStartDate('');
                    setEndDate('');
                    setType(ABSENCE_TYPE.VACATION);
                    setReason('');
                    onSuccess?.();
                },
            }
        );
    };

    return (
        <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 mb-6">
            <h3 className="text-lg font-semibold mb-4">Request Absence</h3>

            <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                    <label className="block text-sm font-semibold mb-2">Start Date</label>
                    <input
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        required
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div>
                    <label className="block text-sm font-semibold mb-2">End Date</label>
                    <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        required
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>
            </div>

            <div className="mb-4">
                <label className="block text-sm font-semibold mb-2">Type</label>
                <select
                    value={type}
                    onChange={(e) => setType(e.target.value as AbsenceType)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    <option value={ABSENCE_TYPE.VACATION}>Vacation</option>
                    <option value={ABSENCE_TYPE.SICK}>Sick Leave</option>
                    <option value={ABSENCE_TYPE.OTHER}>Other</option>
                </select>
            </div>

            <div className="mb-4">
                <label className="block text-sm font-semibold mb-2">Reason (optional)</label>
                <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Enter reason for absence..."
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={3}
                />
            </div>

            <button
                type="submit"
                disabled={isPending || !startDate || !endDate}
                className="bg-blue-500 text-white px-6 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
            >
                {isPending ? 'Submitting...' : 'Request Absence'}
            </button>
        </form>
    );
};

