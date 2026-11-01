import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Empty, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { useCurrentUser } from '@/hooks/use-auth'
import { apiFetch, ApiRequestError } from '@/lib/api-client'
import { hasRole } from '@/lib/auth'
import type { ActiveStatusRequest, WasteCategory, WasteCategoryRequest } from '@/types/api'

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(80, 'At most 80 characters'),
  description: z.string().max(500, 'At most 500 characters'),
})
type FormValues = z.infer<typeof schema>

const categoriesKey = ['waste-categories'] as const

/** Waste categories: read-only for planners, editable by administrators. */
export default function CategoriesPage() {
  const queryClient = useQueryClient()
  const { data: user } = useCurrentUser()
  const isAdmin = hasRole(user, 'ADMIN')
  const [includeInactive, setIncludeInactive] = useState(false)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<WasteCategory | null>(null)

  const categories = useQuery({
    queryKey: [...categoriesKey, { includeInactive }],
    queryFn: () =>
      apiFetch<WasteCategory[]>(
        includeInactive ? '/api/waste-categories' : '/api/waste-categories?active=true',
      ),
  })

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '' },
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: categoriesKey })

  const save = useMutation({
    mutationFn: (v: { id: string | null; body: WasteCategoryRequest }) =>
      v.id
        ? apiFetch<WasteCategory>(`/api/waste-categories/${v.id}`, { method: 'PUT', body: v.body })
        : apiFetch<WasteCategory>('/api/waste-categories', { method: 'POST', body: v.body }),
    onSuccess: () => {
      toast.success(editing ? 'Category updated' : 'Category created')
      setDialogOpen(false)
      void refresh()
    },
    onError: (err: Error) => {
      if (err instanceof ApiRequestError && err.status === 409) {
        setError('name', { message: err.message })
      } else if (err instanceof ApiRequestError && err.status === 400 && err.body?.fieldErrors) {
        for (const fe of err.body.fieldErrors) {
          setError(fe.field as keyof FormValues, { message: fe.message })
        }
      } else {
        toast.error(err.message)
      }
    },
  })

  const toggle = useMutation({
    mutationFn: (c: WasteCategory) =>
      apiFetch<WasteCategory>(`/api/waste-categories/${c.id}/status`, {
        method: 'PATCH',
        body: { active: !c.active } satisfies ActiveStatusRequest,
      }),
    onSuccess: (c) => {
      toast.success(`${c.name} ${c.active ? 'activated' : 'deactivated'}`)
      void refresh()
    },
    onError: (err: Error) => toast.error(err.message),
  })

  const openDialog = (category: WasteCategory | null) => {
    setEditing(category)
    reset({ name: category?.name ?? '', description: category?.description ?? '' })
    setDialogOpen(true)
  }

  const onSubmit = (v: FormValues) => {
    const description = v.description.trim()
    save.mutate({
      id: editing?.id ?? null,
      body: { name: v.name, description: description || undefined },
    })
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Waste categories</h1>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <Switch
              id="show-inactive"
              checked={includeInactive}
              onCheckedChange={setIncludeInactive}
            />
            <Label htmlFor="show-inactive">Show inactive</Label>
          </div>
          {isAdmin && (
            <Button onClick={() => openDialog(null)}>
              <Plus />
              New category
            </Button>
          )}
        </div>
      </div>
      <p className="text-sm text-muted-foreground">
        Quantities for every category are recorded in kilograms.
      </p>
      {!isAdmin && (
        <p className="text-sm text-muted-foreground">
          Only administrators can change waste categories.
        </p>
      )}

      {categories.isPending && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      )}
      {categories.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load categories</AlertTitle>
          <AlertDescription>{categories.error.message}</AlertDescription>
        </Alert>
      )}
      {categories.data && categories.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No waste categories</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {categories.data && categories.data.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Status</TableHead>
                {isAdmin && <TableHead className="text-right">Actions</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {categories.data.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell className="whitespace-normal">{c.description ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant={c.active ? 'default' : 'secondary'}>
                      {c.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </TableCell>
                  {isAdmin && (
                    <TableCell className="space-x-2 text-right">
                      <Button variant="outline" size="sm" onClick={() => openDialog(c)}>
                        Edit
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={toggle.isPending}
                        onClick={() => toggle.mutate(c)}
                      >
                        {c.active ? 'Deactivate' : 'Activate'}
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? 'Edit category' : 'New category'}</DialogTitle>
          </DialogHeader>
          <form
            id="category-form"
            onSubmit={handleSubmit(onSubmit)}
            className="flex flex-col gap-4"
            noValidate
          >
            <div className="flex flex-col gap-2">
              <Label htmlFor="category-name">Name</Label>
              <Input id="category-name" {...register('name')} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="category-description">Description</Label>
              <Textarea id="category-description" {...register('description')} />
              {errors.description && (
                <p className="text-sm text-destructive">{errors.description.message}</p>
              )}
            </div>
          </form>
          <DialogFooter>
            <Button type="submit" form="category-form" disabled={save.isPending}>
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
